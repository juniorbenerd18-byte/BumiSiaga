package com.example.siagabumi.ui.cuaca

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.data.mapper.CuacaMapper
import com.example.siagabumi.data.repository.WilayahRepositoryImpl
import com.example.siagabumi.domain.model.CuacaDetail
import com.example.siagabumi.domain.model.PrakiraanCuaca
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import com.example.siagabumi.domain.repository.WilayahRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CuacaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cuacaRepository: CuacaRepository,
    private val wilayahRepository: WilayahRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val navAdm4: String? = savedStateHandle.get<String>("adm4")?.let { raw ->
        val decoded = try {
            URLDecoder.decode(raw, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            raw
        }
        if (decoded.isBlank() || decoded == "{adm4}") null else decoded
    }

    private val _currentAdm4 = MutableStateFlow<String?>(navAdm4)
    val currentAdm4: StateFlow<String?> = _currentAdm4.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<CuacaDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<CuacaDetail>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(true)
    private val _networkError = MutableStateFlow(false)

    init {
        loadData()
        observeCuacaData()
    }

    private fun loadData() {
        viewModelScope.launch {
            wilayahRepository.ensureWilayahDataSeeded()
            if (userPreferencesRepository != null) {
                userPreferencesRepository.getUserPreferences().collect { prefs ->
                    val adm4 = if (navAdm4 != null && _currentAdm4.value == navAdm4) {
                        navAdm4
                    } else {
                        prefs.userAdm4.ifBlank { WilayahRepositoryImpl.DEFAULT_WILAYAH.kode }
                    }
                    if (_currentAdm4.value != adm4) {
                        _currentAdm4.value = adm4
                        refreshCuaca(adm4, force = false)
                    } else {
                        refreshCuaca(adm4, force = false)
                    }
                }
            } else {
                val initial = _currentAdm4.value
                val resolved = if (initial == null) {
                    wilayahRepository.getDefaultWilayah().kode
                } else {
                    wilayahRepository.resolveToAdm4(initial)
                }
                _currentAdm4.value = resolved
                refreshCuaca(resolved, force = false)
            }
        }
    }

    private fun observeCuacaData() {
        viewModelScope.launch {
            _currentAdm4.flatMapLatest { adm4 ->
                if (adm4 == null) {
                    flowOf(UiState.Loading)
                } else {
                    combine(
                        cuacaRepository.getPrakiraanCuaca(adm4),
                        cuacaRepository.getCacheLastUpdated(adm4),
                        cuacaRepository.isFavorit(adm4),
                        _isRefreshing,
                        _networkError
                    ) { list, lastUpdatedMillis, isFav, refreshing, networkErr ->
                        val currentPrefs = userPreferencesRepository?.getPreferencesSync()
                        val wilayah = if (currentPrefs != null && currentPrefs.userAdm4 == adm4 && currentPrefs.userLocationName.isNotBlank() && currentPrefs.userLocationName != "Jakarta / Nasional") {
                            Wilayah(
                                kode = adm4,
                                nama = currentPrefs.userLocationName.split(",").firstOrNull()?.trim() ?: currentPrefs.userLocationName,
                                level = 4,
                                parentKode = "",
                                namaLengkap = currentPrefs.userLocationName
                            )
                        } else {
                            wilayahRepository.getWilayahByKode(adm4)
                                ?: Wilayah(adm4, "Kemayoran", 4, null, "Kemayoran, Jakarta Pusat")
                        }

                        if (list.isEmpty()) {
                            if (networkErr && !refreshing) {
                                UiState.Error("Gagal mengambil prakiraan cuaca dari BMKG. Periksa koneksi internet.")
                            } else {
                                UiState.Loading
                            }
                        } else {
                            val kondisiSaatIni = CuacaMapper.findKondisiSaatIni(list)
                            val today = LocalDate.now().toString()
                            val hariIniList = list.filter { it.tanggal == today }.ifEmpty { list.take(8) }
                            val ringkasan = CuacaMapper.groupDailyForecast(list)

                            val formattedLastUpdated = lastUpdatedMillis?.let {
                                val instant = Instant.ofEpochMilli(it)
                                DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                                    .withZone(ZoneId.systemDefault())
                                    .format(instant)
                            }

                            val detail = CuacaDetail(
                                lokasi = wilayah,
                                kondisiSaatIni = kondisiSaatIni,
                                prakiraanHariIni3Jam = hariIniList,
                                ringkasanHarian = ringkasan,
                                isFavorit = isFav,
                                lastUpdated = formattedLastUpdated
                            )

                            UiState.Success(
                                data = detail,
                                isFromCache = networkErr,
                                lastUpdated = formattedLastUpdated
                            )
                        }
                    }
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectAdm4(adm4: String) {
        viewModelScope.launch {
            val resolved = wilayahRepository.resolveToAdm4(adm4)
            val wilayah = wilayahRepository.getWilayahByKode(resolved)
            val name = wilayah?.namaLengkap ?: wilayah?.nama ?: resolved
            val (lat, lon) = com.example.siagabumi.util.GeoUtils.getApproximateCoordinates(
                admCode = resolved,
                locationName = name
            )
            userPreferencesRepository?.setUserLocation(
                lat = lat,
                lon = lon,
                name = name,
                adm4 = resolved,
                isGps = false
            )
            _currentAdm4.value = resolved
        }
    }

    fun refresh() {
        val adm4 = _currentAdm4.value ?: return
        refreshCuaca(adm4, force = true)
    }

    private fun refreshCuaca(adm4: String, force: Boolean) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = cuacaRepository.refreshCuaca(adm4, force = force)
            _isRefreshing.value = false
            _networkError.value = result.isFailure
        }
    }

    fun toggleFavorit() {
        val state = _uiState.value
        if (state is UiState.Success) {
            viewModelScope.launch {
                cuacaRepository.toggleFavorit(state.data.lokasi)
            }
        }
    }
}
