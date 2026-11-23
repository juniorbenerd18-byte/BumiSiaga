package com.example.siagabumi.domain.model

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val minMagnitudeAlert: Double = 5.0,
    val nearbyRadiusKm: Int = 0, // 0 means all of Indonesia (unlimited)
    val userLat: Double = -6.2088,
    val userLon: Double = 106.8456,
    val userLocationName: String = "Jakarta / Nasional",
    val userAdm4: String = "31.71.03.1001",
    val isGpsLocation: Boolean = false,
    val language: String = "in", // "in" or "en"
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)
