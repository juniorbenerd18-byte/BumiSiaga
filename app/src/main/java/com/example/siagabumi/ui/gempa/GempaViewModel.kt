package com.example.siagabumi.ui.gempa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.GempaCategory
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.repository.GempaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class GempaScreenData(
    val gempaTerbaru: Gempa? = null,
    val selectedTab: Int = 0, // 0: M 5.0+, 1: Dirasakan
    val gempaTerkiniList: List<Gempa> = emptyList(),
    val gempaDirasakanList: List<Gempa> = emptyList(),
    val minMagnitudeFilter: Double = 0.0,
    val isRefreshing: Boolean = false,
    val isOfflineCached: Boolean = false,
    val lastUpdatedText: String? = null
) {
    val displayedList: List<Gempa>
        get() {
            val list = if (selectedTab == 0) gempaTerkiniList else gempaDirasakanList
            return if (minMagnitudeFilter > 0.0) {
                list.filter { it.magnitude >= minMagnitudeFilter }
            } else {
                list
            }
        }
}

private data class GempaRepoData(
    val terbaru: Gempa?,
    val terkini: List<Gempa>,
    val dirasakan: List<Gempa>,
    val lastUpdatedMillis: Long?
)

private data class GempaUiControls(
    val tab: Int,
    val minMag: Double,
    val refreshing: Boolean,
    val networkError: Boolean
)

@HiltViewModel
class GempaViewModel @Inject constructor(
    private val repository: GempaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<GempaScreenData>>(UiState.Loading)
    val uiState: StateFlow<UiState<GempaScreenData>> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    private val _minMagnitude = MutableStateFlow(0.0)
    private val _isRefreshing = MutableStateFlow(false)
    private val _networkErrorOccurred = MutableStateFlow(false)

    init {
        observeData()
        loadInitialData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val repoFlow = combine(
                repository.getGempaTerbaru(),
                repository.getGempaTerkini(),
                repository.getGempaDirasakan(),
                repository.getCacheLastUpdated(GempaCategory.TERKINI)
            ) { terbaru, terkini, dirasakan, lastUpdatedMillis ->
                GempaRepoData(terbaru, terkini, dirasakan, lastUpdatedMillis)
            }

            val controlsFlow = combine(
                _selectedTab,
                _minMagnitude,
                _isRefreshing,
                _networkErrorOccurred
            ) { tab, minMag, refreshing, networkError ->
                GempaUiControls(tab, minMag, refreshing, networkError)
            }

            combine(repoFlow, controlsFlow) { repo, controls ->
                val hasData = repo.terbaru != null || repo.terkini.isNotEmpty() || repo.dirasakan.isNotEmpty()

                if (!hasData && controls.networkError && !controls.refreshing) {
                    UiState.Error("Gagal terhubung ke BMKG. Periksa koneksi internet Anda.")
                } else if (!hasData && !controls.refreshing) {
                    UiState.Loading
                } else {
                    val formattedLastUpdated = repo.lastUpdatedMillis?.let {
                        val instant = Instant.ofEpochMilli(it)
                        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                            .withZone(ZoneId.systemDefault())
                            .format(instant)
                    }

                    UiState.Success(
                        data = GempaScreenData(
                            gempaTerbaru = repo.terbaru,
                            selectedTab = controls.tab,
                            gempaTerkiniList = repo.terkini,
                            gempaDirasakanList = repo.dirasakan,
                            minMagnitudeFilter = controls.minMag,
                            isRefreshing = controls.refreshing,
                            isOfflineCached = controls.networkError,
                            lastUpdatedText = formattedLastUpdated
                        ),
                        isFromCache = controls.networkError,
                        lastUpdated = formattedLastUpdated
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.refreshAll(force = false)
            _isRefreshing.value = false
            _networkErrorOccurred.value = result.isFailure
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.refreshAll(force = true)
            _isRefreshing.value = false
            _networkErrorOccurred.value = result.isFailure
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setMinMagnitudeFilter(mag: Double) {
        _minMagnitude.value = mag
    }
}
