package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class GetAnimalByIdUseCaseTest {

    @Test
    fun existente_retornaEseAnimal() = runTest {
        val animal = sampleAnimal()
        val repository = FakeAnimalRepository(animal = animal)
        val result = GetAnimalByIdUseCase(repository).invoke(animal.idAnimal)

        assertEquals(animal, result)
        assertEquals(animal.idAnimal, repository.requestedId)
    }

    @Test
    fun noEncontrado_retornaNull() = runTest {
        val repository = FakeAnimalRepository(animal = null)
        val result = GetAnimalByIdUseCase(repository).invoke(9L)

        assertNull(result)
        assertEquals(9L, repository.requestedId)
    }

    @Test
    fun excepcion_sePropaga() = runTest {
        val failure = IllegalStateException("db")
        val repository = FakeAnimalRepository(error = failure)
        var thrown: Throwable? = null

        try {
            GetAnimalByIdUseCase(repository).invoke(3L)
        } catch (exception: IllegalStateException) {
            thrown = exception
        }

        assertSame(failure, thrown)
    }

    private fun sampleAnimal() = Animal(
        idAnimal = 4L,
        especie = EspecieAnimal.CERDO,
        nombre = "Manchita",
        raza = "SIN_ESPECIFICAR",
        edad = 2,
        peso = 80f,
        color = "Blanco",
        fotoUri = null,
        idUsuario = 7L,
    )

    private class FakeAnimalRepository(
        private val animal: Animal? = null,
        private val error: Throwable? = null,
    ) : AnimalRepository {
        var requestedId: Long? = null

        override fun getAllAnimals(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override suspend fun getAnimalById(id: Long): Animal? {
            requestedId = id
            error?.let { throw it }
            return animal
        }

        override suspend fun insertAnimal(animal: Animal): Long = error("unused")

        override suspend fun updateAnimal(animal: Animal) = error("unused")

        override suspend fun deleteAnimal(id: Long) = error("unused")
    }
}
