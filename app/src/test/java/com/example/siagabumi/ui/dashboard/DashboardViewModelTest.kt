package com.example.siagabumi.ui.dashboard

import com.example.siagabumi.data.repository.WilayahRepositoryImpl
import com.example.siagabumi.domain.model.AppThemeMode
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGempaRepo : GempaRepository {
        val terbaruFlow = MutableStateFlow<Gempa?>(null)
        val terkiniFlow = MutableStateFlow<List<Gempa>>(emptyList())

        override fun getGempaTerbaru(): Flow<Gempa?> = terbaruFlow
        override fun getGempaTerkini(): Flow<List<Gempa>> = terkiniFlow
        override fun getGempaDirasakan(): Flow<List<Gempa>> = flowOf(emptyList())
        override fun getGempaById(id: String): Flow<Gempa?> = flowOf(null)
        override fun getCacheLastUpdated(category: GempaCategory): Flow<Long?> = flowOf(null)
        override suspend fun refreshGempaTerbaru(force: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun refreshGempaTerkini(force: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun refreshGempaDirasakan(force: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun refreshAll(force: Boolean): Result<Unit> = Result.success(Unit)
    }

    private class FakeCuacaRepo : CuacaRepository {
        override fun getPrakiraanCuaca(adm4: String): Flow<List<PrakiraanCuaca>> = flowOf(emptyList())
        override fun getCacheLastUpdated(adm4: String): Flow<Long?> = flowOf(null)
        override fun getAllFavorit(): Flow<List<Wilayah>> = flowOf(emptyList())
        override fun isFavorit(adm4: String): Flow<Boolean> = flowOf(false)
        override suspend fun toggleFavorit(wilayah: Wilayah) {}
        override suspend fun refreshCuaca(adm4: String, force: Boolean): Result<Unit> = Result.success(Unit)
    }

    private class FakeWilayahRepo : WilayahRepository {
        override fun searchWilayah(query: String): Flow<List<Wilayah>> = flowOf(emptyList())
        override suspend fun getWilayahByKode(kode: String): Wilayah? = null
        override suspend fun ensureWilayahDataSeeded(onProgress: ((Float) -> Unit)?) {}
        override suspend fun getDefaultWilayah(): Wilayah = WilayahRepositoryImpl.DEFAULT_WILAYAH
        override suspend fun resolveToAdm4(kode: String): String = kode
    }

    private class FakeUserPreferencesRepo : UserPreferencesRepository {
        private val flow = MutableStateFlow(UserPreferences())
        override fun getUserPreferences(): Flow<UserPreferences> = flow
        override suspend fun getPreferencesSync(): UserPreferences = flow.value
        override suspend fun setNotificationsEnabled(enabled: Boolean) { flow.value = flow.value.copy(notificationsEnabled = enabled) }
        override suspend fun setVibrationEnabled(enabled: Boolean) { flow.value = flow.value.copy(vibrationEnabled = enabled) }
        override suspend fun setMinMagnitudeAlert(minMag: Double) { flow.value = flow.value.copy(minMagnitudeAlert = minMag) }
        override suspend fun setNearbyRadiusKm(radiusKm: Int) { flow.value = flow.value.copy(nearbyRadiusKm = radiusKm) }
        override suspend fun setUserLocation(lat: Double, lon: Double, name: String) { flow.value = flow.value.copy(userLat = lat, userLon = lon, userLocationName = name) }
        override suspend fun setLanguage(language: String) { flow.value = flow.value.copy(language = language) }
        override suspend fun setThemeMode(themeMode: AppThemeMode) { flow.value = flow.value.copy(themeMode = themeMode) }
    }

    private fun createDummyGempa(id: String, magnitude: Double, isTsunami: Boolean): Gempa {
        return Gempa(
            id = id,
            tanggal = "23 Nov 2026",
            jam = "14:00:00 WIB",
            dateTimeUtc = "2026-11-23T07:00:00Z",
            formattedLocalTime = "23 Nov 2026, 14:00:00",
            coordinates = "-6.2,106.8",
            latitude = -6.2,
            longitude = 106.8,
            lintang = "6.2 LS",
            bujur = "106.8 BT",
            magnitude = magnitude,
            kedalamanKm = 10,
            kedalamanText = "10 km",
            wilayah = "Jakarta Pusat",
            potensi = if (isTsunami) "Berpotensi tsunami" else "Tidak berpotensi tsunami",
            dirasakan = null,
            shakemapUrl = null,
            isTsunamiWarning = isTsunami,
            category = GempaCategory.TERKINI
        )
    }

    @Test
    fun dashboard_combinesQuakeAndWeather_correctly() = runTest {
        val gempaRepo = FakeGempaRepo()
        val cuacaRepo = FakeCuacaRepo()
        val wilayahRepo = FakeWilayahRepo()
        val userPrefsRepo = FakeUserPreferencesRepo()

        val terbaru = createDummyGempa("1", 5.2, false)
        val terkiniList = listOf(
            createDummyGempa("2", 5.5, false),
            createDummyGempa("3", 6.2, false)
        )
        gempaRepo.terbaruFlow.value = terbaru
        gempaRepo.terkiniFlow.value = terkiniList

        val viewModel = DashboardViewModel(gempaRepo, cuacaRepo, wilayahRepo, userPrefsRepo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data

        assertEquals(terbaru, data.gempaTerbaru)
        assertEquals(2, data.m5PlusCount)
        assertTrue(data.hasSignificantQuake) // because of 6.2 in list
        assertFalse(data.isTsunamiAlert)
    }
}
