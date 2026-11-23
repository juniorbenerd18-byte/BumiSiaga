package com.example.siagabumi.ui.wilayah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import com.example.siagabumi.domain.repository.WilayahRepository
import com.example.siagabumi.util.GpsLocationResult
import com.example.siagabumi.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class WilayahSearchViewModel @Inject constructor(
    private val wilayahRepository: WilayahRepository,
    private val cuacaRepository: CuacaRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val locationHelper: LocationHelper? = null
) : ViewModel() {

    val popularWilayahList: List<Wilayah> = listOf(
        Wilayah("31.71.03.1001", "Kemayoran", 4, "31.71.03", "Kemayoran, Jakarta Pusat, DKI Jakarta"),
        Wilayah("31.74.04.1001", "Kebayoran Baru", 4, "31.74.04", "Kebayoran Baru, Jakarta Selatan, DKI Jakarta"),
        Wilayah("32.73.01.1001", "Bandung", 4, "32.73.01", "Sukasari, Kota Bandung, Jawa Barat"),
        Wilayah("32.76.01.1001", "Depok", 4, "32.76.01", "Pancoran Mas, Kota Depok, Jawa Barat"),
        Wilayah("32.75.01.1001", "Bekasi", 4, "32.75.01", "Bekasi Timur, Kota Bekasi, Jawa Barat"),
        Wilayah("32.71.01.1001", "Bogor", 4, "32.71.01", "Bogor Tengah, Kota Bogor, Jawa Barat"),
        Wilayah("35.78.01.1001", "Surabaya", 4, "35.78.01", "Genteng, Kota Surabaya, Jawa Timur"),
        Wilayah("33.74.01.1001", "Semarang", 4, "33.74.01", "Semarang Tengah, Kota Semarang, Jawa Tengah"),
        Wilayah("34.71.01.1001", "Yogyakarta", 4, "34.71.01", "Danurejan, Kota Yogyakarta, D.I. Yogyakarta"),
        Wilayah("51.71.01.1001", "Denpasar", 4, "51.71.01", "Denpasar Barat, Kota Denpasar, Bali"),
        Wilayah("12.71.01.1001", "Medan", 4, "12.71.01", "Medan Kota, Kota Medan, Sumatera Utara"),
        Wilayah("73.71.01.1001", "Makassar", 4, "73.71.01", "Ujung Pandang, Kota Makassar, Sulawesi Selatan"),
        Wilayah("16.71.01.1001", "Palembang", 4, "16.71.01", "Ilir Barat I, Kota Palembang, Sumatera Selatan"),
        Wilayah("64.71.01.1001", "Balikpapan", 4, "64.71.01", "Balikpapan Kota, Kota Balikpapan, Kalimantan Timur")
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _isDetectingGps = MutableStateFlow(false)
    val isDetectingGps: StateFlow<Boolean> = _isDetectingGps.asStateFlow()

    init {
        viewModelScope.launch {
            _isImporting.value = true
            wilayahRepository.ensureWilayahDataSeeded()
            _isImporting.value = false
        }
    }

    val searchResults: StateFlow<List<Wilayah>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.length < 2) {
                flowOf(emptyList())
            } else {
                wilayahRepository.searchWilayah(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun resolveAdm4(wilayah: Wilayah, onResolved: (String) -> Unit) {
        viewModelScope.launch {
            val adm4 = wilayahRepository.resolveToAdm4(wilayah.kode)
            userPreferencesRepository?.setUserLocation(
                lat = 0.0,
                lon = 0.0,
                name = wilayah.namaLengkap,
                adm4 = adm4,
                isGps = false
            )
            cuacaRepository?.refreshCuaca(adm4, force = true)
            onResolved(adm4)
        }
    }

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
                    val resolved = wilayahRepository.resolveGpsToAdm4(result)
                    val adm4 = resolved?.kode ?: "31.71.03.1001"
                    userPreferencesRepository?.setUserLocation(
                        lat = result.latitude,
                        lon = result.longitude,
                        name = result.locationName,
                        adm4 = adm4,
                        isGps = true
                    )
                    cuacaRepository?.refreshCuaca(adm4, force = true)
                    val cityName = result.locationName
                        .split(",")
                        .firstOrNull()
                        ?.replace("Kota ", "")
                        ?.replace("Kabupaten ", "")
                        ?.replace("Kab. ", "")
                        ?.trim()
                    if (!cityName.isNullOrBlank()) {
                        _searchQuery.value = cityName
                    }
                }
                onResult(result)
            } finally {
                _isDetectingGps.value = false
            }
        }
    }
}
