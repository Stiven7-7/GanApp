package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.AnimalDefaults
import com.proyecto.ganapp.domain.model.EspecieAnimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalDetailPresentationTest {

    @Test
    fun especie_vaca() {
        assertEquals("Vaca", animalSpeciesLabel(EspecieAnimal.VACA))
    }

    @Test
    fun especie_cerdo() {
        assertEquals("Cerdo", animalSpeciesLabel(EspecieAnimal.CERDO))
    }

    @Test
    fun especie_oveja() {
        assertEquals("Oveja", animalSpeciesLabel(EspecieAnimal.OVEJA))
    }

    @Test
    fun especie_sinEspecificar() {
        assertEquals("Sin especificar", animalSpeciesLabel(EspecieAnimal.SIN_ESPECIFICAR))
    }

    @Test
    fun razaReal_esVisible() {
        assertTrue(shouldShowBreed("Holstein"))
        assertEquals("Raza · Holstein", animalBreedLabel("Holstein"))
    }

    @Test
    fun razaSentinel_seOculta() {
        assertFalse(shouldShowBreed(AnimalDefaults.RAZA_SIN_ESPECIFICAR))
    }

    @Test
    fun filas_edadNulaNoAparece() {
        val labels = buildAnimalDetailRows(animal(edad = null, peso = 80f, color = "Blanco"))
            .map { it.label }
        assertFalse(labels.contains("Edad"))
    }

    @Test
    fun filas_edadCeroEsVisible() {
        assertEquals(
            listOf(AnimalDetailRow("Edad", "0 años")),
            buildAnimalDetailRows(animal(edad = 0)),
        )
    }

    @Test
    fun filas_edadUnoUsaSingular() {
        assertEquals(
            listOf(AnimalDetailRow("Edad", "1 año")),
            buildAnimalDetailRows(animal(edad = 1)),
        )
    }

    @Test
    fun filas_pesoNuloNoAparece() {
        val labels = buildAnimalDetailRows(animal(edad = 2, peso = null, color = "Blanco"))
            .map { it.label }
        assertFalse(labels.contains("Peso"))
    }

    @Test
    fun filas_pesoCeroEsVisible() {
        assertEquals(
            listOf(AnimalDetailRow("Peso", "0.0 kg")),
            buildAnimalDetailRows(animal(peso = 0f)),
        )
    }

    @Test
    fun filas_colorNuloOBlancoNoAparece() {
        assertTrue(buildAnimalDetailRows(animal(color = null)).isEmpty())
        assertTrue(buildAnimalDetailRows(animal(color = "")).isEmpty())
        assertTrue(buildAnimalDetailRows(animal(color = "   ")).isEmpty())
    }

    @Test
    fun filas_soloDatosPresentes() {
        assertEquals(
            listOf(
                AnimalDetailRow("Edad", "4 años"),
                AnimalDetailRow("Peso", "420.0 kg"),
                AnimalDetailRow("Color", "Negro y blanco"),
            ),
            buildAnimalDetailRows(
                animal(edad = 4, peso = 420f, color = "Negro y blanco"),
            ),
        )
    }

    private fun animal(
        edad: Int? = null,
        peso: Float? = null,
        color: String? = null,
    ) = Animal(
        idAnimal = 1L,
        especie = EspecieAnimal.VACA,
        nombre = "Lola",
        raza = "Holstein",
        edad = edad,
        peso = peso,
        color = color,
        fotoUri = null,
        idUsuario = 7L,
    )
}
