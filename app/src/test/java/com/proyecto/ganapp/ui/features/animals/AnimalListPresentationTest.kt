package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.R
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalListPresentationTest {

    @Test
    fun metadata_todoNulo_retornaNull() {
        assertNull(buildAnimalMetadata(animal()))
    }

    @Test
    fun metadata_soloEdad() {
        assertEquals("2 años", buildAnimalMetadata(animal(edad = 2)))
    }

    @Test
    fun metadata_soloPeso() {
        assertEquals("80.0 kg", buildAnimalMetadata(animal(peso = 80f)))
    }

    @Test
    fun metadata_soloColor() {
        assertEquals("Blanco", buildAnimalMetadata(animal(color = "Blanco")))
    }

    @Test
    fun metadata_completa() {
        assertEquals(
            "2 años • 80.0 kg • Blanco",
            buildAnimalMetadata(animal(edad = 2, peso = 80f, color = "Blanco")),
        )
    }

    @Test
    fun metadata_edadCeroSeMuestra() {
        assertEquals("0 años", buildAnimalMetadata(animal(edad = 0)))
    }

    @Test
    fun metadata_pesoCeroSeMuestra() {
        assertEquals("0.0 kg", buildAnimalMetadata(animal(peso = 0f)))
    }

    @Test
    fun metadata_colorVacioNoSeMuestra() {
        assertNull(buildAnimalMetadata(animal(color = "")))
    }

    @Test
    fun metadata_colorEnBlancoNoSeMuestra() {
        assertNull(buildAnimalMetadata(animal(color = "   ")))
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
        edad: Int? = null,
        peso: Float? = null,
        color: String? = null,
    ) = Animal(
        idAnimal = 1L,
        especie = EspecieAnimal.VACA,
        nombre = "Manchita",
        raza = "SIN_ESPECIFICAR",
        edad = edad,
        peso = peso,
        color = color,
        fotoUri = null,
        idUsuario = 7L,
    )
}
