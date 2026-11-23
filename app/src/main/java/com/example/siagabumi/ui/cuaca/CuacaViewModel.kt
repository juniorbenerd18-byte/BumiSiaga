package com.example.siagabumi.ui.cuaca

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.data.mapper.CuacaMapper
import com.example.siagabumi.domain.model.CuacaDetail
import com.example.siagabumi.domain.model.PrakiraanCuaca
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
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
    private val wilayahRepository: WilayahRepository
) : ViewModel() {

    private val navAdm4: String? = savedStateHandle.get<String>("adm4")?.let {
        try {
            URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            it
        }
    }

    private val _currentAdm4 = MutableStateFlow<String?>(navAdm4)
    val currentAdm4: StateFlow<String?> = _currentAdm4.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<CuacaDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<CuacaDetail>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    private val _networkError = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            wilayahRepository.ensureWilayahDataSeeded()
            if (_currentAdm4.value == null) {
                val defaultWilayah = wilayahRepository.getDefaultWilayah()
                _currentAdm4.value = defaultWilayah.kode
            }
        }
        observeCuacaData()
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
                        val wilayah = wilayahRepository.getWilayahByKode(adm4)
                            ?: Wilayah(adm4, "Kemayoran", 4, null, "Kemayoran, Jakarta Pusat")

                        if (list.isEmpty() && networkErr && !refreshing) {
                            UiState.Error("Gagal mengambil prakiraan cuaca dari BMKG.")
                        } else if (list.isEmpty() && !refreshing) {
                            UiState.Loading
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

        // Also trigger refresh when adm4 is set or changed
        viewModelScope.launch {
            _currentAdm4.collect { adm4 ->
                if (adm4 != null) {
                    refreshCuaca(adm4, force = false)
                }
            }
        }
    }

    fun selectAdm4(adm4: String) {
        _currentAdm4.value = adm4
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
