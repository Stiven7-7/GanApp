package com.proyecto.ganapp.domain.usecase.animal

enum class RegisterAnimalValidationError {
    SPECIES_REQUIRED,
    SPECIES_NOT_ALLOWED,
    NAME_REQUIRED,
    INVALID_AGE,
    INVALID_WEIGHT,
    INVALID_USER,
}
