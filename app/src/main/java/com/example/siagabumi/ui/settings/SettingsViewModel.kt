package com.example.siagabumi.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.domain.model.AppThemeMode
import com.example.siagabumi.domain.model.UserPreferences
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import com.example.siagabumi.util.GpsLocationResult
import com.example.siagabumi.util.LocationHelper
import com.example.siagabumi.worker.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationHelper: LocationHelper? = null
) : ViewModel() {

    private val _isDetectingGps = MutableStateFlow(false)
    val isDetectingGps: StateFlow<Boolean> = _isDetectingGps.asStateFlow()

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository
        .getUserPreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun hasLocationPermission(): Boolean {
        return locationHelper?.hasLocationPermission() == true
    }

    fun isGpsEnabled(): Boolean {
        return locationHelper?.isGpsEnabled() == true
    }

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
                        lat = result.latitude,
                        lon = result.longitude,
                        name = result.locationName,
                        isGps = true
                    )
                }
                onResult(result)
            } finally {
                _isDetectingGps.value = false
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setVibrationEnabled(enabled)
        }
    }

    fun setMinMagnitude(mag: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setMinMagnitudeAlert(mag)
        }
    }

    fun setNearbyRadius(radiusKm: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setNearbyRadiusKm(radiusKm)
        }
    }

    fun setUserLocation(lat: Double, lon: Double, name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserLocation(lat, lon, name)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguage(lang)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun triggerTestAlert(context: Context) {
        val currentVibrate = preferences.value.vibrationEnabled
        NotificationHelper.showTestNotification(context, vibrate = currentVibrate)
    }
}
