package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.R
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalListPresentationTest {

    @Test
    fun metadata_sinDatos_retornaNull() {
        assertNull(buildAnimalCardMetadata(animal()))
    }

    @Test
    fun metadata_soloRaza() {
        assertEquals(
            AnimalCardMetadata(lineOne = "Holstein", lineTwo = null),
            buildAnimalCardMetadata(animal(raza = "Holstein")),
        )
    }

    @Test
    fun metadata_razaSentinelSeOmite() {
        assertNull(buildAnimalCardMetadata(animal(raza = "SIN_ESPECIFICAR")))
    }

    @Test
    fun metadata_soloEdad() {
        assertEquals(
            AnimalCardMetadata(lineOne = "2 años", lineTwo = null),
            buildAnimalCardMetadata(animal(edad = 2)),
        )
    }

    @Test
    fun metadata_edadUnoUsaSingular() {
        assertEquals(
            AnimalCardMetadata(lineOne = "1 año", lineTwo = null),
            buildAnimalCardMetadata(animal(edad = 1)),
        )
    }

    @Test
    fun metadata_soloPeso() {
        assertEquals(
            AnimalCardMetadata(lineOne = null, lineTwo = "80.0 kg"),
            buildAnimalCardMetadata(animal(peso = 80f)),
        )
    }

    @Test
    fun metadata_soloColor() {
        assertEquals(
            AnimalCardMetadata(lineOne = null, lineTwo = "Blanco"),
            buildAnimalCardMetadata(animal(color = "Blanco")),
        )
    }

    @Test
    fun metadata_razaYEdad() {
        assertEquals(
            AnimalCardMetadata(lineOne = "Holstein • 2 años", lineTwo = null),
            buildAnimalCardMetadata(animal(raza = "Holstein", edad = 2)),
        )
    }

    @Test
    fun metadata_edadYPeso() {
        assertEquals(
            AnimalCardMetadata(lineOne = "2 años", lineTwo = "80.0 kg"),
            buildAnimalCardMetadata(animal(edad = 2, peso = 80f)),
        )
    }

    @Test
    fun metadata_completa() {
        assertEquals(
            AnimalCardMetadata(
                lineOne = "Holstein • 4 años",
                lineTwo = "420.0 kg • Negro y blanco",
            ),
            buildAnimalCardMetadata(
                animal(raza = "Holstein", edad = 4, peso = 420f, color = "Negro y blanco"),
            ),
        )
    }

    @Test
    fun metadata_edadCeroSeMuestra() {
        assertEquals(
            AnimalCardMetadata(lineOne = "0 años", lineTwo = null),
            buildAnimalCardMetadata(animal(edad = 0)),
        )
    }

    @Test
    fun metadata_pesoCeroSeMuestra() {
        assertEquals(
            AnimalCardMetadata(lineOne = null, lineTwo = "0.0 kg"),
            buildAnimalCardMetadata(animal(peso = 0f)),
        )
    }

    @Test
    fun metadata_colorVacioNoSeMuestra() {
        assertNull(buildAnimalCardMetadata(animal(color = "")))
    }

    @Test
    fun metadata_colorEnBlancoNoSeMuestra() {
        assertNull(buildAnimalCardMetadata(animal(color = "   ")))
    }

    @Test
    fun imagen_vaca() {
        assertEquals(R.drawable.def_vaca, getAnimalImageRes(EspecieAnimal.VACA))
    }

    @Test
    fun imagen_cerdo() {
        assertEquals(R.drawable.def_cerdo, getAnimalImageRes(EspecieAnimal.CERDO))
    }

    @Test
    fun imagen_oveja() {
        assertEquals(R.drawable.def_oveja, getAnimalImageRes(EspecieAnimal.OVEJA))
    }

    @Test
    fun imagen_sinEspecificar_esNull() {
        assertNull(getAnimalImageRes(EspecieAnimal.SIN_ESPECIFICAR))
    }

    private fun animal(
        raza: String = "SIN_ESPECIFICAR",
        edad: Int? = null,
        peso: Float? = null,
        color: String? = null,
    ) = Animal(
        idAnimal = 1L,
        especie = EspecieAnimal.VACA,
        nombre = "Manchita",
        raza = raza,
        edad = edad,
        peso = peso,
        color = color,
        fotoUri = null,
        idUsuario = 7L,
    )
}
