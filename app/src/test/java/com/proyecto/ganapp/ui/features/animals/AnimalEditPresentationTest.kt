package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalEditPresentationTest {

    @Test
    fun especieReal_quedaSeleccionada() {
        assertEquals(EspecieAnimal.VACA, initialEditableSpecies(EspecieAnimal.VACA))
        assertEquals(EspecieAnimal.CERDO, initialEditableSpecies(EspecieAnimal.CERDO))
        assertEquals(EspecieAnimal.OVEJA, initialEditableSpecies(EspecieAnimal.OVEJA))
    }

    @Test
    fun sinEspecificar_noQuedaSeleccionado() {
        assertNull(initialEditableSpecies(EspecieAnimal.SIN_ESPECIFICAR))
    }

    @Test
    fun edad_vaciaOReal() {
        assertEquals("", formatEditableAge(null))
        assertEquals("0", formatEditableAge(0))
        assertEquals("4", formatEditableAge(4))
    }

    @Test
    fun peso_vacioCeroOReal() {
        assertEquals("", formatEditableWeight(null))
        assertEquals("0", formatEditableWeight(0f))
        assertEquals("80", formatEditableWeight(80f))
        assertEquals("12.5", formatEditableWeight(12.5f))
    }

    @Test
    fun mensajesDeValidacion() {
        assertEquals(
            "Seleccione una especie.",
            updateValidationMessage(UpdateAnimalValidationError.SPECIES_REQUIRED),
        )
        assertEquals(
            "La especie seleccionada no es válida.",
            updateValidationMessage(UpdateAnimalValidationError.SPECIES_NOT_ALLOWED),
        )
        assertEquals(
            "El nombre es obligatorio.",
            updateValidationMessage(UpdateAnimalValidationError.NAME_REQUIRED),
        )
        assertEquals(
            "La edad no es válida.",
            updateValidationMessage(UpdateAnimalValidationError.INVALID_AGE),
        )
        assertEquals(
            "El peso no es válido.",
            updateValidationMessage(UpdateAnimalValidationError.INVALID_WEIGHT),
        )
    }
}
