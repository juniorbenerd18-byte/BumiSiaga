package com.example.siagabumi.ui.cuaca

import androidx.lifecycle.SavedStateHandle
import com.example.siagabumi.domain.model.PrakiraanCuaca
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CuacaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCuacaRepository : CuacaRepository {
        val cuacaFlow = MutableStateFlow<List<PrakiraanCuaca>>(emptyList())
        val favoritFlow = MutableStateFlow(false)
        var shouldFail = false
        var toggleCalled = false

        override fun getPrakiraanCuaca(adm4: String): Flow<List<PrakiraanCuaca>> = cuacaFlow
        override fun getCacheLastUpdated(adm4: String): Flow<Long?> = flowOf(null)
        override suspend fun refreshCuaca(adm4: String, force: Boolean): Result<Unit> =
            if (shouldFail) Result.failure(Exception("Network error")) else Result.success(Unit)

        override fun isFavorit(adm4: String): Flow<Boolean> = favoritFlow

        override suspend fun toggleFavorit(wilayah: Wilayah) {
            toggleCalled = true
            favoritFlow.value = !favoritFlow.value
        }

        override fun getAllFavorit(): Flow<List<Wilayah>> = flowOf(emptyList())
    }

    private class FakeWilayahRepository : WilayahRepository {
        val defaultWilayah = Wilayah("31.71.03.1001", "Kemayoran", 4, "31.71.03", "Kemayoran, Jakarta Pusat")
        override fun searchWilayah(query: String): Flow<List<Wilayah>> = flowOf(listOf(defaultWilayah))
        override suspend fun getWilayahByKode(kode: String): Wilayah = defaultWilayah
        override suspend fun ensureWilayahDataSeeded(onProgress: ((Float) -> Unit)?) {}
        override suspend fun getDefaultWilayah(): Wilayah = defaultWilayah
    }

    private fun createDummyPrakiraan(adm4: String): PrakiraanCuaca {
        return PrakiraanCuaca(
            id = "${adm4}_2026-11-23T02:00:00Z",
            adm4 = adm4,
            datetimeUtc = "2026-11-23T02:00:00Z",
            localDatetime = "2026-11-23 09:00:00",
            formattedHour = "09:00",
            suhu = 30,
            tutupanAwan = 70,
            curahHujan = 0.0,
            weatherCode = 1,
            weatherDesc = "Cerah Berawan",
            weatherDescEn = "Partly Cloudy",
            arahAngin = "E",
            kecepatanAngin = 5.0,
            kelembapan = 65,
            imageUrl = "https://example.com/icon.svg",
            tanggal = "2026-11-23"
        )
    }

    @Test
    fun loadCuaca_success_emitsSuccessWithData() = runTest {
        val cuacaRepo = FakeCuacaRepository()
        val wilayahRepo = FakeWilayahRepository()
        cuacaRepo.cuacaFlow.value = listOf(createDummyPrakiraan("31.71.03.1001"))

        val savedStateHandle = SavedStateHandle(mapOf("adm4" to "31.71.03.1001"))
        val viewModel = CuacaViewModel(savedStateHandle, cuacaRepo, wilayahRepo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("Kemayoran", data.lokasi.nama)
        assertEquals(30, data.kondisiSaatIni?.suhu)
    }

    @Test
    fun loadCuaca_failureWithoutCache_emitsError() = runTest {
        val cuacaRepo = FakeCuacaRepository().apply { shouldFail = true }
        val wilayahRepo = FakeWilayahRepository()

        val savedStateHandle = SavedStateHandle(mapOf("adm4" to "31.71.03.1001"))
        val viewModel = CuacaViewModel(savedStateHandle, cuacaRepo, wilayahRepo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
    }

    @Test
    fun toggleFavorit_callsRepository() = runTest {
        val cuacaRepo = FakeCuacaRepository()
        val wilayahRepo = FakeWilayahRepository()
        cuacaRepo.cuacaFlow.value = listOf(createDummyPrakiraan("31.71.03.1001"))

        val savedStateHandle = SavedStateHandle(mapOf("adm4" to "31.71.03.1001"))
        val viewModel = CuacaViewModel(savedStateHandle, cuacaRepo, wilayahRepo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.toggleFavorit()
        advanceUntilIdle()

        assertTrue(cuacaRepo.toggleCalled)
    }
}
