package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.AnimalEntity
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.AnimalDefaults
import com.proyecto.ganapp.domain.model.EspecieAnimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalMapperTest {

    @Test
    fun entityADominio_copiaTodosLosCampos() {
        val entity = AnimalEntity(
            idAnimal = 9L,
            especie = EspecieAnimal.CERDO,
            nombre = "Rosado",
            raza = "Duroc",
            edad = 2,
            peso = 80.5f,
            color = "Rosa",
            idUsuario = 4L,
            fotoUri = "content://ganapp/rosado",
        )

        val animal = entity.toDomain()

        assertEquals(9L, animal.idAnimal)
        assertEquals(EspecieAnimal.CERDO, animal.especie)
        assertEquals("Rosado", animal.nombre)
        assertEquals("Duroc", animal.raza)
        assertEquals(2, animal.edad)
        assertEquals(80.5f, animal.peso)
        assertEquals("Rosa", animal.color)
        assertEquals("content://ganapp/rosado", animal.fotoUri)
        assertEquals(4L, animal.idUsuario)
    }

    @Test
    fun dominioAEntity_copiaTodosLosCampos() {
        val animal = Animal(
            idAnimal = 11L,
            especie = EspecieAnimal.OVEJA,
            nombre = "Nube",
            raza = "Merino",
            edad = 1,
            peso = 42f,
            color = "Blanco",
            fotoUri = "content://ganapp/nube",
            idUsuario = 8L,
        )

        val entity = animal.toEntity()

        assertEquals(11L, entity.idAnimal)
        assertEquals(EspecieAnimal.OVEJA, entity.especie)
        assertEquals("Nube", entity.nombre)
        assertEquals("Merino", entity.raza)
        assertEquals(1, entity.edad)
        assertEquals(42f, entity.peso)
        assertEquals("Blanco", entity.color)
        assertEquals("content://ganapp/nube", entity.fotoUri)
        assertEquals(8L, entity.idUsuario)
    }

    @Test
    fun nulos_sePreservanEnAmbasDirecciones() {
        val animal = Animal(
            idAnimal = 3L,
            especie = EspecieAnimal.VACA,
            nombre = "Lola",
            raza = "Brahman",
            edad = null,
            peso = null,
            color = null,
            fotoUri = null,
            idUsuario = 1L,
        )

        val entity = animal.toEntity()
        val roundTrip = entity.toDomain()

        assertNull(entity.edad)
        assertNull(entity.peso)
        assertNull(entity.color)
        assertNull(entity.fotoUri)
        assertNull(roundTrip.edad)
        assertNull(roundTrip.peso)
        assertNull(roundTrip.color)
        assertNull(roundTrip.fotoUri)
    }

    @Test
    fun razaHistorica_sePreservaExacta() {
        listOf("Brahman", "Holstein").forEach { raza ->
            val animal = sample(raza = raza).toEntity().toDomain()
            assertEquals(raza, animal.raza)
        }
    }

    @Test
    fun especieYRaza_sonIndependientes() {
        val legacy = Animal(
            especie = EspecieAnimal.SIN_ESPECIFICAR,
            nombre = "Manchita",
            raza = "Holstein",
            edad = 4,
            peso = 300f,
            color = "Pinto",
            fotoUri = null,
            idUsuario = 2L,
        )
        val nuevo = Animal(
            especie = EspecieAnimal.VACA,
            nombre = "Lola",
            raza = AnimalDefaults.RAZA_SIN_ESPECIFICAR,
            edad = null,
            peso = null,
            color = null,
            fotoUri = null,
            idUsuario = 2L,
        )

        assertEquals(EspecieAnimal.SIN_ESPECIFICAR, legacy.toEntity().toDomain().especie)
        assertEquals("Holstein", legacy.toEntity().toDomain().raza)
        assertEquals(EspecieAnimal.VACA, nuevo.toEntity().toDomain().especie)
        assertEquals(AnimalDefaults.RAZA_SIN_ESPECIFICAR, nuevo.toEntity().toDomain().raza)
    }

    @Test
    fun roundTrip_mantieneEquivalencia() {
        val original = Animal(
            idAnimal = 15L,
            especie = EspecieAnimal.VACA,
            nombre = "Manchita",
            raza = " Holstein ",
            edad = 0,
            peso = 0f,
            color = "",
            fotoUri = "content://ganapp/manchita",
            idUsuario = 6L,
        )

        assertEquals(original, original.toEntity().toDomain())
    }

    private fun sample(raza: String): Animal {
        return Animal(
            idAnimal = 1L,
            especie = EspecieAnimal.VACA,
            nombre = "Lola",
            raza = raza,
            edad = 5,
            peso = 400f,
            color = "Blanco",
            fotoUri = null,
            idUsuario = 1L,
        )
    }
}
