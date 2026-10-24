package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalValidationError

fun initialEditableSpecies(especie: EspecieAnimal): EspecieAnimal? {
    return when (especie) {
        EspecieAnimal.SIN_ESPECIFICAR -> null
        EspecieAnimal.VACA,
        EspecieAnimal.CERDO,
        EspecieAnimal.OVEJA,
        -> especie
    }
}

fun formatEditableAge(edad: Int?): String {
    return edad?.toString().orEmpty()
}

fun formatEditableWeight(peso: Float?): String {
    if (peso == null) return ""
    if (peso.isFinite() && peso % 1f == 0f) return peso.toInt().toString()
    return peso.toString()
}

fun updateValidationMessage(error: UpdateAnimalValidationError): String {
    return when (error) {
        UpdateAnimalValidationError.SPECIES_REQUIRED -> "Seleccione una especie."
        UpdateAnimalValidationError.SPECIES_NOT_ALLOWED -> "La especie seleccionada no es válida."
        UpdateAnimalValidationError.NAME_REQUIRED -> "El nombre es obligatorio."
        UpdateAnimalValidationError.INVALID_AGE -> "La edad no es válida."
        UpdateAnimalValidationError.INVALID_WEIGHT -> "El peso no es válido."
    }
}
