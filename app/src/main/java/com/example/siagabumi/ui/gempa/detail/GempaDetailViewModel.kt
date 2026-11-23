package com.example.siagabumi.ui.gempa.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.repository.GempaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class GempaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    gempaRepository: GempaRepository
) : ViewModel() {

    private val rawGempaId: String = savedStateHandle.get<String>("gempaId") ?: ""
    private val gempaId: String = try {
        URLDecoder.decode(rawGempaId, StandardCharsets.UTF_8.toString())
    } catch (_: Exception) {
        rawGempaId
    }

    val uiState: StateFlow<UiState<Gempa>> = gempaRepository.getGempaById(gempaId)
        .map { gempa ->
            if (gempa != null) {
                UiState.Success(gempa)
            } else {
                UiState.Error("Data detail gempa tidak ditemukan.")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )
}
