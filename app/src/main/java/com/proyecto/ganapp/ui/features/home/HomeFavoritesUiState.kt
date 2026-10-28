package com.proyecto.ganapp.ui.features.home

import com.proyecto.ganapp.domain.model.Animal

sealed interface HomeFavoritesUiState {
    data object Loading : HomeFavoritesUiState

    data class Success(
        val animals: List<Animal>,
    ) : HomeFavoritesUiState

    data class Error(
        val cause: Throwable? = null,
    ) : HomeFavoritesUiState
}
