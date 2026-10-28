package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.Animal

enum class AnimalSpeciesFilter {
    ALL,
    VACA,
    CERDO,
    OVEJA,
}

sealed interface AnimalsListUiState {
    data object Loading : AnimalsListUiState
    data object Empty : AnimalsListUiState
    data object NoResults : AnimalsListUiState

    data class Success(
        val animals: List<Animal>,
    ) : AnimalsListUiState

    data class Error(
        val cause: Throwable? = null,
    ) : AnimalsListUiState
}
