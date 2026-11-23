package com.example.siagabumi.ui.gempa.detail

import androidx.lifecycle.SavedStateHandle
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.GempaCategory
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.repository.GempaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalCoroutinesApi::class)
class GempaDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRepo(private val gempaMap: Map<String, Gempa>) : GempaRepository {
        override fun getGempaTerbaru(): Flow<Gempa?> = flowOf(null)
        override fun getGempaTerkini(): Flow<List<Gempa>> = flowOf(emptyList())
        override fun getGempaDirasakan(): Flow<List<Gempa>> = flowOf(emptyList())
        override fun getGempaById(id: String): Flow<Gempa?> = flowOf(gempaMap[id])
        override fun getCacheLastUpdated(category: GempaCategory): Flow<Long?> = flowOf(null)
        override suspend fun refreshGempaTerbaru(force: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun refreshGempaTerkini(force: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun refreshGempaDirasakan(force: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun refreshAll(force: Boolean): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun loadGempaById_found_emitsSuccess() = runTest {
        val targetId = "2026-11-22T07:54:39+00:00_3.12,126.57"
        val dummy = Gempa(
            id = targetId,
            tanggal = "22 Nov 2026",
            jam = "14:54:39 WIB",
            dateTimeUtc = "2026-11-22T07:54:39+00:00",
            formattedLocalTime = "22 Nov 2026, 14:54:39",
            coordinates = "3.12,126.57",
            latitude = 3.12,
            longitude = 126.57,
            lintang = "3.12 LU",
            bujur = "126.57 BT",
            magnitude = 5.4,
            kedalamanKm = 10,
            kedalamanText = "10 km",
            wilayah = "Melonguane",
            potensi = "Tidak berpotensi tsunami",
            dirasakan = null,
            shakemapUrl = null,
            isTsunamiWarning = false,
            category = GempaCategory.TERKINI
        )
        val fakeRepo = FakeRepo(mapOf(targetId to dummy))

        val encodedId = URLEncoder.encode(targetId, StandardCharsets.UTF_8.toString())
        val savedStateHandle = SavedStateHandle(mapOf("gempaId" to encodedId))

        val viewModel = GempaDetailViewModel(savedStateHandle, fakeRepo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(dummy, (state as UiState.Success).data)
    }

    @Test
    fun loadGempaById_notFound_emitsError() = runTest {
        val fakeRepo = FakeRepo(emptyMap())
        val savedStateHandle = SavedStateHandle(mapOf("gempaId" to "unknown_id"))

        val viewModel = GempaDetailViewModel(savedStateHandle, fakeRepo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
    }

    @Test
    fun loadGempaById_rawIdWithPlus_emitsSuccess() = runTest {
        val targetId = "2026-11-22T07:54:39+00:00_3.12,126.57"
        val dummy = Gempa(
            id = targetId,
            tanggal = "22 Nov 2026",
            jam = "14:54:39 WIB",
            dateTimeUtc = "2026-11-22T07:54:39+00:00",
            formattedLocalTime = "22 Nov 2026, 14:54:39",
            coordinates = "3.12,126.57",
            latitude = 3.12,
            longitude = 126.57,
            lintang = "3.12 LU",
            bujur = "126.57 BT",
            magnitude = 5.4,
            kedalamanKm = 10,
            kedalamanText = "10 km",
            wilayah = "Melonguane",
            potensi = "Tidak berpotensi tsunami",
            dirasakan = null,
            shakemapUrl = null,
            isTsunamiWarning = false,
            category = GempaCategory.TERKINI
        )
        val fakeRepo = FakeRepo(mapOf(targetId to dummy))

        val savedStateHandle = SavedStateHandle(mapOf("gempaId" to targetId))

        val viewModel = GempaDetailViewModel(savedStateHandle, fakeRepo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(dummy, (state as UiState.Success).data)
    }
}
