package com.example.siagabumi.ui.favorit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.CuacaRepository
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritViewModel @Inject constructor(
    private val cuacaRepository: CuacaRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    val favoritList: StateFlow<List<Wilayah>> = cuacaRepository.getAllFavorit()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectFavoritAsActive(wilayah: Wilayah) {
        viewModelScope.launch {
            userPreferencesRepository?.setUserLocation(
                lat = 0.0,
                lon = 0.0,
                name = wilayah.namaLengkap,
                adm4 = wilayah.kode,
                isGps = false
            )
        }
    }

    fun removeFavorit(wilayah: Wilayah) {
        viewModelScope.launch {
            cuacaRepository.toggleFavorit(wilayah)
        }
    }
}
