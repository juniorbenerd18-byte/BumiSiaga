package com.example.siagabumi.domain.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<out T>(
        val data: T,
        val isFromCache: Boolean = false,
        val lastUpdated: String? = null
    ) : UiState<T>
    data class Error(
        val message: String
    ) : UiState<Nothing>
}
