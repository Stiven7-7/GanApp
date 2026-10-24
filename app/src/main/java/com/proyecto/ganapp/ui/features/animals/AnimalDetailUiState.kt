package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.Animal

sealed interface AnimalDetailUiState {
    data object Loading : AnimalDetailUiState

    data class Success(
        val animal: Animal,
    ) : AnimalDetailUiState

    data object NotFound : AnimalDetailUiState

    data class Error(
        val cause: Throwable? = null,
    ) : AnimalDetailUiState
}
