package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalValidationError

sealed interface AnimalEditUiState {
    data object Loading : AnimalEditUiState

    data class Ready(
        val animal: Animal,
    ) : AnimalEditUiState

    data class Saving(
        val animal: Animal,
    ) : AnimalEditUiState

    data class ValidationError(
        val animal: Animal,
        val errors: Set<UpdateAnimalValidationError>,
    ) : AnimalEditUiState

    data class Success(
        val animal: Animal,
    ) : AnimalEditUiState

    data object NotFound : AnimalEditUiState

    data class Error(
        val cause: Throwable? = null,
        val animal: Animal? = null,
    ) : AnimalEditUiState
}
