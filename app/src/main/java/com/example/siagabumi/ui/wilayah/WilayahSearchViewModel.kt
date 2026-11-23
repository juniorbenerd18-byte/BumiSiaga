package com.example.siagabumi.ui.wilayah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.domain.model.Wilayah
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
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val locationHelper: LocationHelper? = null
) : ViewModel() {

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
                    userPreferencesRepository?.setUserLocation(
                        result.latitude,
                        result.longitude,
                        result.locationName
                    )
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
