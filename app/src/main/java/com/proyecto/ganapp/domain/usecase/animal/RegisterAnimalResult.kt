package com.proyecto.ganapp.domain.usecase.animal

sealed interface RegisterAnimalResult {
    data class Success(
        val animalId: Long,
    ) : RegisterAnimalResult

    data class ValidationError(
        val errors: Set<RegisterAnimalValidationError>,
    ) : RegisterAnimalResult

    data class Failure(
        val cause: Throwable? = null,
    ) : RegisterAnimalResult
}
