package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal

sealed interface UpdateAnimalResult {
    data class Success(
        val animal: Animal,
    ) : UpdateAnimalResult

    data class ValidationError(
        val errors: Set<UpdateAnimalValidationError>,
    ) : UpdateAnimalResult

    data object NotFound : UpdateAnimalResult

    data class Failure(
        val cause: Throwable? = null,
    ) : UpdateAnimalResult
}
