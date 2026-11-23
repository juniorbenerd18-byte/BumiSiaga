package com.example.siagabumi.ui.settings

import com.example.siagabumi.domain.model.AppThemeMode
import com.example.siagabumi.domain.model.UserPreferences
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeUserPreferencesRepository()
        viewModel = SettingsViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setNotificationsEnabled_updatesPreference() = runTest {
        viewModel.setNotificationsEnabled(false)
        advanceUntilIdle()
        assertFalse(fakeRepository.getPreferencesSync().notificationsEnabled)

        viewModel.setNotificationsEnabled(true)
        advanceUntilIdle()
        assertTrue(fakeRepository.getPreferencesSync().notificationsEnabled)
    }

    @Test
    fun setVibrationEnabled_updatesPreference() = runTest {
        viewModel.setVibrationEnabled(false)
        advanceUntilIdle()
        assertFalse(fakeRepository.getPreferencesSync().vibrationEnabled)
    }

    @Test
    fun setMinMagnitude_updatesPreference() = runTest {
        viewModel.setMinMagnitude(6.0)
        advanceUntilIdle()
        assertEquals(6.0, fakeRepository.getPreferencesSync().minMagnitudeAlert, 0.01)
    }

    @Test
    fun setNearbyRadius_updatesPreference() = runTest {
        viewModel.setNearbyRadius(100)
        advanceUntilIdle()
        assertEquals(100, fakeRepository.getPreferencesSync().nearbyRadiusKm)
    }

    @Test
    fun setLanguage_updatesPreference() = runTest {
        viewModel.setLanguage("en")
        advanceUntilIdle()
        assertEquals("en", fakeRepository.getPreferencesSync().language)
    }

    @Test
    fun setThemeMode_updatesPreference() = runTest {
        viewModel.setThemeMode(AppThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(AppThemeMode.DARK, fakeRepository.getPreferencesSync().themeMode)
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val flow = MutableStateFlow(UserPreferences())

    override fun getUserPreferences(): Flow<UserPreferences> = flow
    override suspend fun getPreferencesSync(): UserPreferences = flow.value

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        flow.value = flow.value.copy(notificationsEnabled = enabled)
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        flow.value = flow.value.copy(vibrationEnabled = enabled)
    }

    override suspend fun setMinMagnitudeAlert(minMag: Double) {
        flow.value = flow.value.copy(minMagnitudeAlert = minMag)
    }

    override suspend fun setNearbyRadiusKm(radiusKm: Int) {
        flow.value = flow.value.copy(nearbyRadiusKm = radiusKm)
    }

    override suspend fun setUserLocation(lat: Double, lon: Double, name: String, adm4: String, isGps: Boolean) {
        flow.value = flow.value.copy(userLat = lat, userLon = lon, userLocationName = name, userAdm4 = adm4, isGpsLocation = isGps)
    }

    override suspend fun setLanguage(language: String) {
        flow.value = flow.value.copy(language = language)
    }

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        flow.value = flow.value.copy(themeMode = themeMode)
    }
}
