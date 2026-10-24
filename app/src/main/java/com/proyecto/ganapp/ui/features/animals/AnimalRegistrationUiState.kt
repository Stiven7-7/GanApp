package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.usecase.animal.RegisterAnimalValidationError

sealed interface AnimalRegistrationUiState {
    data object Idle : AnimalRegistrationUiState
    data object Loading : AnimalRegistrationUiState

    data class ValidationError(
        val errors: Set<RegisterAnimalValidationError>,
    ) : AnimalRegistrationUiState

    data class Failure(
        val cause: Throwable? = null,
    ) : AnimalRegistrationUiState

    data class Success(
        val animalId: Long,
    ) : AnimalRegistrationUiState
}
