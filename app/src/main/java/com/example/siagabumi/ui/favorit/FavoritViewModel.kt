package com.example.siagabumi.ui.favorit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritViewModel @Inject constructor(
    private val cuacaRepository: CuacaRepository
) : ViewModel() {

    val favoritList: StateFlow<List<Wilayah>> = cuacaRepository.getAllFavorit()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeFavorit(wilayah: Wilayah) {
        viewModelScope.launch {
            cuacaRepository.toggleFavorit(wilayah)
        }
    }
}
