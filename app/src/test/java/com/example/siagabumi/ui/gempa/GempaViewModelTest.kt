package com.example.siagabumi.ui.gempa

import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.GempaCategory
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.repository.GempaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class GempaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGempaRepository : GempaRepository {
        val terbaruFlow = MutableStateFlow<Gempa?>(null)
        val terkiniFlow = MutableStateFlow<List<Gempa>>(emptyList())
        val dirasakanFlow = MutableStateFlow<List<Gempa>>(emptyList())
        val lastUpdatedFlow = MutableStateFlow<Long?>(null)

        var shouldFailRefresh = false

        override fun getGempaTerbaru(): Flow<Gempa?> = terbaruFlow
        override fun getGempaTerkini(): Flow<List<Gempa>> = terkiniFlow
        override fun getGempaDirasakan(): Flow<List<Gempa>> = dirasakanFlow
        override fun getGempaById(id: String): Flow<Gempa?> = flowOf(null)
        override fun getCacheLastUpdated(category: GempaCategory): Flow<Long?> = lastUpdatedFlow

        override suspend fun refreshGempaTerbaru(force: Boolean): Result<Unit> =
            if (shouldFailRefresh) Result.failure(Exception("Network error")) else Result.success(Unit)

        override suspend fun refreshGempaTerkini(force: Boolean): Result<Unit> =
            if (shouldFailRefresh) Result.failure(Exception("Network error")) else Result.success(Unit)

        override suspend fun refreshGempaDirasakan(force: Boolean): Result<Unit> =
            if (shouldFailRefresh) Result.failure(Exception("Network error")) else Result.success(Unit)

        override suspend fun refreshAll(force: Boolean): Result<Unit> =
            if (shouldFailRefresh) Result.failure(Exception("Network error")) else Result.success(Unit)
    }

    private fun createDummyGempa(id: String, magnitude: Double, category: GempaCategory): Gempa {
        return Gempa(
            id = id,
            tanggal = "23 Nov 2026",
            jam = "12:00:00 WIB",
            dateTimeUtc = "2026-11-23T05:00:00Z",
            formattedLocalTime = "23 Nov 2026, 12:00:00",
            coordinates = "-6.2,106.8",
            latitude = -6.2,
            longitude = 106.8,
            lintang = "6.2 LS",
            bujur = "106.8 BT",
            magnitude = magnitude,
            kedalamanKm = 10,
            kedalamanText = "10 km",
            wilayah = "Jakarta",
            potensi = "Tidak berpotensi tsunami",
            dirasakan = null,
            shakemapUrl = null,
            isTsunamiWarning = false,
            category = category
        )
    }

    @Test
    fun loadInitialData_successState_emitsSuccessWithData() = runTest {
        val fakeRepo = FakeGempaRepository()
        val dummyTerbaru = createDummyGempa("1", 5.5, GempaCategory.TERBARU)
        val dummyTerkini = listOf(
            createDummyGempa("2", 5.2, GempaCategory.TERKINI),
            createDummyGempa("3", 6.8, GempaCategory.TERKINI)
        )

        fakeRepo.terbaruFlow.value = dummyTerbaru
        fakeRepo.terkiniFlow.value = dummyTerkini

        val viewModel = GempaViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val successData = (state as UiState.Success).data
        assertEquals(dummyTerbaru, successData.gempaTerbaru)
        assertEquals(2, successData.gempaTerkiniList.size)
        assertEquals(2, successData.displayedList.size)
    }

    @Test
    fun loadInitialData_failureWithoutCache_emitsErrorState() = runTest {
        val fakeRepo = FakeGempaRepository().apply {
            shouldFailRefresh = true
        }

        val viewModel = GempaViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
    }

    @Test
    fun filterMagnitude_filtersCorrectly() = runTest {
        val fakeRepo = FakeGempaRepository()
        val dummyTerkini = listOf(
            createDummyGempa("1", 5.1, GempaCategory.TERKINI),
            createDummyGempa("2", 5.8, GempaCategory.TERKINI),
            createDummyGempa("3", 6.5, GempaCategory.TERKINI)
        )
        fakeRepo.terkiniFlow.value = dummyTerkini

        val viewModel = GempaViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.setMinMagnitudeFilter(5.5)
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(2, state.data.displayedList.size)
        assertTrue(state.data.displayedList.all { it.magnitude >= 5.5 })
    }

    @Test
    fun selectTab_switchesBetweenTerkiniAndDirasakan() = runTest {
        val fakeRepo = FakeGempaRepository()
        fakeRepo.terkiniFlow.value = listOf(createDummyGempa("1", 5.1, GempaCategory.TERKINI))
        fakeRepo.dirasakanFlow.value = listOf(
            createDummyGempa("2", 4.2, GempaCategory.DIRASAKAN),
            createDummyGempa("3", 4.8, GempaCategory.DIRASAKAN)
        )

        val viewModel = GempaViewModel(fakeRepo)
        advanceUntilIdle()

        // Default tab 0: Terkini
        var state = viewModel.uiState.value as UiState.Success
        assertEquals(1, state.data.displayedList.size)

        // Switch to tab 1: Dirasakan
        viewModel.selectTab(1)
        advanceUntilIdle()

        state = viewModel.uiState.value as UiState.Success
        assertEquals(2, state.data.displayedList.size)
    }
}
