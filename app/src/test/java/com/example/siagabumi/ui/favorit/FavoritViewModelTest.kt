package com.example.siagabumi.ui.favorit

import com.example.siagabumi.domain.model.PrakiraanCuaca
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
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
class FavoritViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCuacaRepo : CuacaRepository {
        val favFlow = MutableStateFlow<List<Wilayah>>(emptyList())
        var toggleCalled = false

        override fun getPrakiraanCuaca(adm4: String): Flow<List<PrakiraanCuaca>> = flowOf(emptyList())
        override fun getCacheLastUpdated(adm4: String): Flow<Long?> = flowOf(null)
        override suspend fun refreshCuaca(adm4: String, force: Boolean): Result<Unit> = Result.success(Unit)
        override fun isFavorit(adm4: String): Flow<Boolean> = flowOf(false)
        override suspend fun toggleFavorit(wilayah: Wilayah) {
            toggleCalled = true
        }
        override fun getAllFavorit(): Flow<List<Wilayah>> = favFlow
    }

    @Test
    fun favoritList_emitsDataAndDeletesCorrectly() = runTest {
        val repo = FakeCuacaRepo()
        val dummy = Wilayah("31.71.03.1001", "Kemayoran", 4, null, "Kemayoran, Jakarta Pusat")
        repo.favFlow.value = listOf(dummy)

        val viewModel = FavoritViewModel(repo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.favoritList.collect()
        }
        advanceUntilIdle()

        assertEquals(1, viewModel.favoritList.value.size)
        assertEquals(dummy, viewModel.favoritList.value[0])

        viewModel.removeFavorit(dummy)
        advanceUntilIdle()

        assertTrue(repo.toggleCalled)
    }
}
