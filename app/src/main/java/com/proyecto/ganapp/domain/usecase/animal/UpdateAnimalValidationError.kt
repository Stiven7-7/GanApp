package com.proyecto.ganapp.domain.usecase.animal

enum class UpdateAnimalValidationError {
    SPECIES_REQUIRED,
    SPECIES_NOT_ALLOWED,
    NAME_REQUIRED,
    INVALID_AGE,
    INVALID_WEIGHT,
}
