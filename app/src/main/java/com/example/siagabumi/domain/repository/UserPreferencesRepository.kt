package com.example.siagabumi.domain.repository

import com.example.siagabumi.domain.model.AppThemeMode
import com.example.siagabumi.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun getPreferencesSync(): UserPreferences
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setVibrationEnabled(enabled: Boolean)
    suspend fun setMinMagnitudeAlert(minMag: Double)
    suspend fun setNearbyRadiusKm(radiusKm: Int)
    suspend fun setUserLocation(
        lat: Double,
        lon: Double,
        name: String,
        adm4: String = "31.71.03.1001",
        isGps: Boolean = false
    )
    suspend fun setLanguage(language: String)
    suspend fun setThemeMode(themeMode: AppThemeMode)
}
