package com.example.siagabumi.ui.gempa.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.repository.GempaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GempaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gempaRepository: GempaRepository
) : ViewModel() {

    private val rawGempaId: String = savedStateHandle.get<String>("gempaId").orEmpty()
    private val gempaId: String = run {
        val decoded = try {
            if (rawGempaId.contains("%")) {
                URLDecoder.decode(rawGempaId, StandardCharsets.UTF_8.toString())
            } else {
                rawGempaId
            }
        } catch (_: Exception) {
            rawGempaId
        }
        decoded.trim()
    }

    private val _refreshTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            gempaRepository.refreshAll(force = false)
        }
    }

    val uiState: StateFlow<UiState<Gempa>> = _refreshTrigger.flatMapLatest {
        gempaRepository.getGempaById(gempaId)
            .flatMapLatest { gempa ->
                if (gempa != null) {
                    flowOf(UiState.Success(gempa))
                } else {
                    val normalized = gempaId.replace(" ", "+")
                    if (normalized != gempaId) {
                        gempaRepository.getGempaById(normalized).flatMapLatest { altGempa ->
                            if (altGempa != null) {
                                flowOf(UiState.Success(altGempa))
                            } else {
                                fallbackSearchAll()
                            }
                        }
                    } else {
                        fallbackSearchAll()
                    }
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    private fun fallbackSearchAll() = combine(
        gempaRepository.getGempaTerbaru(),
        gempaRepository.getGempaTerkini(),
        gempaRepository.getGempaDirasakan()
    ) { terbaru, terkini, dirasakan ->
        val allGempa = (listOfNotNull(terbaru) + terkini + dirasakan).distinctBy { it.id }
        val target = gempaId.trim()
        val match = allGempa.find { it.id == target }
            ?: allGempa.find { it.id.replace(" ", "+") == target.replace(" ", "+") }
            ?: allGempa.find { it.id.replace("+", " ") == target.replace("+", " ") }
            ?: allGempa.find { 
                val c1 = it.id.replace(" ", "").replace("+", "")
                val c2 = target.replace(" ", "").replace("+", "")
                c1.isNotEmpty() && c1 == c2
            }
            ?: allGempa.find {
                val coords = it.coordinates.replace(" ", "")
                coords.isNotEmpty() && target.contains(coords)
            }
            ?: if (target.isEmpty()) terbaru ?: terkini.firstOrNull() else null

        if (match != null) {
            UiState.Success(match)
        } else {
            UiState.Error("Data detail gempa tidak ditemukan.")
        }
    }

    fun retry() {
        viewModelScope.launch {
            gempaRepository.refreshAll(force = true)
            _refreshTrigger.value += 1
        }
    }
}
