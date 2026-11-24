package com.example.siagabumi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.data.mapper.CuacaMapper
import com.example.siagabumi.data.repository.WilayahRepositoryImpl
import com.example.siagabumi.domain.model.CuacaDetail
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.GempaCategory
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    val cuacaDetailList: List<CuacaDetail> = emptyList(),
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
    val cuacaDetailList: List<CuacaDetail>,
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

    private var hasAutoCheckedLocation = false

    fun hasLocationPermission(): Boolean = locationHelper?.hasLocationPermission() == true

    fun onScreenEntered(onRequestPermission: (Boolean) -> Unit) {
        if (hasAutoCheckedLocation) return
        hasAutoCheckedLocation = true

        if (hasLocationPermission()) {
            detectGpsLocation()
        } else {
            onRequestPermission(true)
        }
    }

    fun detectGpsLocation(onResult: ((GpsLocationResult?) -> Unit)? = null) {
        val helper = locationHelper ?: run {
            onResult?.invoke(null)
            return
        }
        viewModelScope.launch {
            _isDetectingGps.value = true
            try {
                val result = helper.getCurrentLocation()
                if (result != null) {
                    val resolved = wilayahRepository.resolveGpsToAdm4(result)
                    val adm4 = resolved?.kode ?: "31.71.03.1001"
                    userPreferencesRepository.setUserLocation(
                        lat = result.latitude,
                        lon = result.longitude,
                        name = result.locationName,
                        adm4 = adm4,
                        isGps = true
                    )
                    cuacaRepository.refreshCuaca(adm4, force = true)
                }
                onResult?.invoke(result)
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
            combine(
                userPreferencesRepository.getUserPreferences(),
                cuacaRepository.getAllFavorit()
            ) { prefs, favoritList ->
                prefs to favoritList
            }.flatMapLatest { (prefs, favoritList) ->
                val activeAdm4 = prefs.userAdm4.ifBlank { WilayahRepositoryImpl.DEFAULT_WILAYAH.kode }

                val activeWilayah = if (prefs.userLocationName.isNotBlank() && prefs.userLocationName != "Jakarta / Nasional") {
                    Wilayah(
                        kode = activeAdm4,
                        nama = prefs.userLocationName.split(",").firstOrNull()?.trim() ?: prefs.userLocationName,
                        level = 4,
                        parentKode = "",
                        namaLengkap = prefs.userLocationName
                    )
                } else {
                    wilayahRepository.getWilayahByKode(activeAdm4) ?: WilayahRepositoryImpl.DEFAULT_WILAYAH
                }

                val allLocations = listOf(activeWilayah) + favoritList.filter { it.kode != activeWilayah.kode }

                val weatherFlowList = if (allLocations.isEmpty()) {
                    flowOf(emptyList<CuacaDetail>())
                } else {
                    combine(allLocations.map { loc ->
                        cuacaRepository.getPrakiraanCuaca(loc.kode).map { forecastList ->
                            val kondisiSaatIni = CuacaMapper.findKondisiSaatIni(forecastList)
                            val today = LocalDate.now().toString()
                            val hariIniList = forecastList.filter { it.tanggal == today }.ifEmpty { forecastList.take(8) }
                            val ringkasan = CuacaMapper.groupDailyForecast(forecastList)
                            CuacaDetail(
                                lokasi = loc,
                                kondisiSaatIni = kondisiSaatIni,
                                prakiraanHariIni3Jam = hariIniList,
                                ringkasanHarian = ringkasan,
                                isFavorit = favoritList.any { it.kode == loc.kode }
                            )
                        }
                    }) { it.toList() }
                }

                val dataFlow = combine(
                    gempaRepository.getGempaTerbaru(),
                    gempaRepository.getGempaTerkini(),
                    weatherFlowList,
                    gempaRepository.getCacheLastUpdated(GempaCategory.TERKINI),
                    userPreferencesRepository.getUserPreferences()
                ) { terbaru, terkini, weatherDetails, lastUpdatedMillis, currentPrefs ->
                    DashboardDataState(terbaru, terkini, weatherDetails, lastUpdatedMillis, currentPrefs)
                }

                val statusFlow = combine(
                    _isRefreshing,
                    _networkError
                ) { refreshing, networkError ->
                    DashboardStatusState(refreshing, networkError)
                }

                combine(dataFlow, statusFlow) { data, status ->
                    val hasQuakeData = data.terbaru != null || data.terkini.isNotEmpty()
                    val hasCuacaData = data.cuacaDetailList.any { it.kondisiSaatIni != null }

                    if (!hasQuakeData && !hasCuacaData && status.networkError && !status.refreshing) {
                        UiState.Error("Gagal memuat dashboard. Periksa koneksi internet.")
                    } else if (!hasQuakeData && !hasCuacaData && !status.refreshing) {
                        UiState.Loading
                    } else {
                        val cuacaDetail = data.cuacaDetailList.firstOrNull()
                        val effectiveLatestQuake = data.terbaru ?: data.terkini.firstOrNull()
                        val isTsunami = (effectiveLatestQuake?.isTsunamiWarning == true) || data.terkini.any { it.isTsunamiWarning }

                        val formattedLastUpdated = data.lastUpdatedMillis?.let {
                            val instant = Instant.ofEpochMilli(it)
                            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(instant)
                        }

                        UiState.Success(
                            data = DashboardScreenData(
                                gempaTerbaru = effectiveLatestQuake,
                                gempaTerkiniList = data.terkini,
                                cuacaDetail = cuacaDetail,
                                cuacaDetailList = data.cuacaDetailList,
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

    fun toggleFavorit(wilayah: Wilayah) {
        viewModelScope.launch {
            cuacaRepository.toggleFavorit(wilayah)
        }
    }

    fun setActiveLocation(wilayah: Wilayah) {
        viewModelScope.launch {
            val (lat, lon) = com.example.siagabumi.util.GeoUtils.getApproximateCoordinates(
                admCode = wilayah.kode,
                locationName = wilayah.namaLengkap
            )
            userPreferencesRepository.setUserLocation(
                lat = lat,
                lon = lon,
                name = wilayah.namaLengkap,
                adm4 = wilayah.kode,
                isGps = false
            )
            cuacaRepository.refreshCuaca(wilayah.kode, force = true)
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            wilayahRepository.ensureWilayahDataSeeded()
            val r1 = gempaRepository.refreshAll(force = false)
            val prefs = userPreferencesRepository.getPreferencesSync()
            val activeAdm4 = if (prefs.userAdm4.isNotBlank()) prefs.userAdm4 else WilayahRepositoryImpl.DEFAULT_WILAYAH.kode
            val r2 = cuacaRepository.refreshCuaca(activeAdm4, force = false)
            try {
                val favList = cuacaRepository.getAllFavorit().first()
                favList.forEach { fav ->
                    if (fav.kode != activeAdm4) {
                        cuacaRepository.refreshCuaca(fav.kode, force = false)
                    }
                }
            } catch (_: Exception) {}
            _isRefreshing.value = false
            _networkError.value = r1.isFailure && r2.isFailure
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val r1 = gempaRepository.refreshAll(force = true)
            val prefs = userPreferencesRepository.getPreferencesSync()
            val activeAdm4 = if (prefs.userAdm4.isNotBlank()) prefs.userAdm4 else WilayahRepositoryImpl.DEFAULT_WILAYAH.kode
            val r2 = cuacaRepository.refreshCuaca(activeAdm4, force = true)
            try {
                val favList = cuacaRepository.getAllFavorit().first()
                favList.forEach { fav ->
                    if (fav.kode != activeAdm4) {
                        cuacaRepository.refreshCuaca(fav.kode, force = true)
                    }
                }
            } catch (_: Exception) {}
            _isRefreshing.value = false
            _networkError.value = r1.isFailure && r2.isFailure
        }
    }
}
