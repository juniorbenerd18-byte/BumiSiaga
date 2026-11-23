package com.example.siagabumi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.data.mapper.CuacaMapper
import com.example.siagabumi.data.repository.WilayahRepositoryImpl
import com.example.siagabumi.domain.model.CuacaDetail
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.GempaCategory
import com.example.siagabumi.domain.model.PrakiraanCuaca
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.model.UserPreferences
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
import com.example.siagabumi.domain.repository.GempaRepository
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import com.example.siagabumi.domain.repository.WilayahRepository
import com.example.siagabumi.util.GpsLocationResult
import com.example.siagabumi.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardScreenData(
    val gempaTerbaru: Gempa? = null,
    val gempaTerkiniList: List<Gempa> = emptyList(),
    val cuacaDetail: CuacaDetail? = null,
    val isTsunamiAlert: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOfflineCached: Boolean = false,
    val lastUpdatedText: String? = null,
    val userPreferences: UserPreferences = UserPreferences()
) {
    val m5PlusCount: Int
        get() = gempaTerkiniList.count { it.magnitude >= 5.0 }

    val hasSignificantQuake: Boolean
        get() = (gempaTerbaru?.magnitude ?: 0.0) >= 6.0 || gempaTerkiniList.any { it.magnitude >= 6.0 }
}

private data class DashboardDataState(
    val terbaru: Gempa?,
    val terkini: List<Gempa>,
    val cuacaList: List<PrakiraanCuaca>,
    val lastUpdatedMillis: Long?,
    val userPreferences: UserPreferences
)

private data class DashboardStatusState(
    val refreshing: Boolean,
    val networkError: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val gempaRepository: GempaRepository,
    private val cuacaRepository: CuacaRepository,
    private val wilayahRepository: WilayahRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationHelper: LocationHelper? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardScreenData>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardScreenData>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    private val _networkError = MutableStateFlow(false)
    private val _isDetectingGps = MutableStateFlow(false)
    val isDetectingGps: StateFlow<Boolean> = _isDetectingGps.asStateFlow()

    fun hasLocationPermission(): Boolean = locationHelper?.hasLocationPermission() == true

    fun detectGpsLocation(onResult: (GpsLocationResult?) -> Unit) {
        val helper = locationHelper ?: run {
            onResult(null)
            return
        }
        viewModelScope.launch {
            _isDetectingGps.value = true
            try {
                val result = helper.getCurrentLocation()
                if (result != null) {
                    userPreferencesRepository.setUserLocation(
                        result.latitude,
                        result.longitude,
                        result.locationName
                    )
                }
                onResult(result)
            } finally {
                _isDetectingGps.value = false
            }
        }
    }

    init {
        loadData()
        observeDashboard()
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            cuacaRepository.getAllFavorit().flatMapLatest { favoritList ->
                val primaryWilayah = favoritList.firstOrNull() ?: WilayahRepositoryImpl.DEFAULT_WILAYAH

                val dataFlow = combine(
                    gempaRepository.getGempaTerbaru(),
                    gempaRepository.getGempaTerkini(),
                    cuacaRepository.getPrakiraanCuaca(primaryWilayah.kode),
                    gempaRepository.getCacheLastUpdated(GempaCategory.TERKINI),
                    userPreferencesRepository.getUserPreferences()
                ) { terbaru, terkini, cuacaList, lastUpdatedMillis, prefs ->
                    DashboardDataState(terbaru, terkini, cuacaList, lastUpdatedMillis, prefs)
                }

                val statusFlow = combine(
                    _isRefreshing,
                    _networkError
                ) { refreshing, networkError ->
                    DashboardStatusState(refreshing, networkError)
                }

                combine(dataFlow, statusFlow) { data, status ->
                    val hasQuakeData = data.terbaru != null || data.terkini.isNotEmpty()
                    val hasCuacaData = data.cuacaList.isNotEmpty()

                    if (!hasQuakeData && !hasCuacaData && status.networkError && !status.refreshing) {
                        UiState.Error("Gagal memuat dashboard. Periksa koneksi internet.")
                    } else if (!hasQuakeData && !hasCuacaData && !status.refreshing) {
                        UiState.Loading
                    } else {
                        val cuacaDetail = if (data.cuacaList.isNotEmpty()) {
                            val kondisiSaatIni = CuacaMapper.findKondisiSaatIni(data.cuacaList)
                            val today = LocalDate.now().toString()
                            val hariIniList = data.cuacaList.filter { it.tanggal == today }.ifEmpty { data.cuacaList.take(8) }
                            val ringkasan = CuacaMapper.groupDailyForecast(data.cuacaList)
                            CuacaDetail(
                                lokasi = primaryWilayah,
                                kondisiSaatIni = kondisiSaatIni,
                                prakiraanHariIni3Jam = hariIniList,
                                ringkasanHarian = ringkasan,
                                isFavorit = favoritList.any { it.kode == primaryWilayah.kode }
                            )
                        } else null

                        val isTsunami = (data.terbaru?.isTsunamiWarning == true) || data.terkini.any { it.isTsunamiWarning }

                        val formattedLastUpdated = data.lastUpdatedMillis?.let {
                            val instant = Instant.ofEpochMilli(it)
                            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(instant)
                        }

                        UiState.Success(
                            data = DashboardScreenData(
                                gempaTerbaru = data.terbaru,
                                gempaTerkiniList = data.terkini,
                                cuacaDetail = cuacaDetail,
                                isTsunamiAlert = isTsunami,
                                isRefreshing = status.refreshing,
                                isOfflineCached = status.networkError,
                                lastUpdatedText = formattedLastUpdated,
                                userPreferences = data.userPreferences
                            ),
                            isFromCache = status.networkError,
                            lastUpdated = formattedLastUpdated
                        )
                    }
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            wilayahRepository.ensureWilayahDataSeeded()
            val r1 = gempaRepository.refreshAll(force = false)
            val defaultWilayah = WilayahRepositoryImpl.DEFAULT_WILAYAH
            val r2 = cuacaRepository.refreshCuaca(defaultWilayah.kode, force = false)
            _isRefreshing.value = false
            _networkError.value = r1.isFailure && r2.isFailure
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val r1 = gempaRepository.refreshAll(force = true)
            val defaultWilayah = WilayahRepositoryImpl.DEFAULT_WILAYAH
            val r2 = cuacaRepository.refreshCuaca(defaultWilayah.kode, force = true)
            _isRefreshing.value = false
            _networkError.value = r1.isFailure && r2.isFailure
        }
    }
}
