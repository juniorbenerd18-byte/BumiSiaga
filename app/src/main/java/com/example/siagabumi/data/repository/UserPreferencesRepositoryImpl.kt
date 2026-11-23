package com.example.siagabumi.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.siagabumi.domain.model.AppThemeMode
import com.example.siagabumi.domain.model.UserPreferences
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getUserPreferences(): Flow<UserPreferences> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(readPreferences())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(readPreferences())
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override suspend fun getPreferencesSync(): UserPreferences = readPreferences()

    private fun readPreferences(): UserPreferences {
        val notif = prefs.getBoolean(KEY_NOTIF, true)
        val vibrate = prefs.getBoolean(KEY_VIBRATE, true)
        val minMag = prefs.getFloat(KEY_MIN_MAG, 5.0f).toDouble()
        val radius = prefs.getInt(KEY_RADIUS, 0)
        val lat = prefs.getFloat(KEY_LAT, -6.2088f).toDouble()
        val lon = prefs.getFloat(KEY_LON, 106.8456f).toDouble()
        val locName = prefs.getString(KEY_LOC_NAME, "Jakarta / Nasional") ?: "Jakarta / Nasional"
        val lang = prefs.getString(KEY_LANG, "in") ?: "in"
        val themeStr = prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val theme = try {
            AppThemeMode.valueOf(themeStr)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }

        return UserPreferences(
            notificationsEnabled = notif,
            vibrationEnabled = vibrate,
            minMagnitudeAlert = minMag,
            nearbyRadiusKm = radius,
            userLat = lat,
            userLon = lon,
            userLocationName = locName,
            language = lang,
            themeMode = theme
        )
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF, enabled).apply()
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
    }

    override suspend fun setMinMagnitudeAlert(minMag: Double) {
        prefs.edit().putFloat(KEY_MIN_MAG, minMag.toFloat()).apply()
    }

    override suspend fun setNearbyRadiusKm(radiusKm: Int) {
        prefs.edit().putInt(KEY_RADIUS, radiusKm).apply()
    }

    override suspend fun setUserLocation(lat: Double, lon: Double, name: String) {
        prefs.edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LON, lon.toFloat())
            .putString(KEY_LOC_NAME, name)
            .apply()
    }

    override suspend fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANG, language).apply()
    }

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME, themeMode.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "siagabumi_user_preferences"
        private const val KEY_NOTIF = "pref_notifications_enabled"
        private const val KEY_VIBRATE = "pref_vibration_enabled"
        private const val KEY_MIN_MAG = "pref_min_magnitude"
        private const val KEY_RADIUS = "pref_nearby_radius"
        private const val KEY_LAT = "pref_user_lat"
        private const val KEY_LON = "pref_user_lon"
        private const val KEY_LOC_NAME = "pref_user_loc_name"
        private const val KEY_LANG = "pref_language"
        private const val KEY_THEME = "pref_theme_mode"
    }
}
