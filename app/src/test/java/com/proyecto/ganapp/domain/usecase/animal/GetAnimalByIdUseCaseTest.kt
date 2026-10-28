package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class GetAnimalByIdUseCaseTest {

    @Test
    fun existente_retornaEseAnimal() = runTest {
        val animal = sampleAnimal()
        val repository = FakeAnimalRepository(animal = animal)
        val result = useCase(repository).invoke(animal.idAnimal)

        assertEquals(animal, result)
        assertSame(animal, result)
        assertEquals(animal.idAnimal, repository.requestedId)
    }

    @Test
    fun propietario_recibeElAnimalCompleto() = runTest {
        val animal = sampleAnimal()
        val result = useCase(FakeAnimalRepository(animal = animal)).invoke(animal.idAnimal)

        assertEquals(4L, result?.idAnimal)
        assertEquals(EspecieAnimal.CERDO, result?.especie)
        assertEquals("Manchita", result?.nombre)
        assertEquals("SIN_ESPECIFICAR", result?.raza)
        assertEquals(2, result?.edad)
        assertEquals(80f, result?.peso)
        assertEquals("Blanco", result?.color)
        assertNull(result?.fotoUri)
        assertEquals(7L, result?.idUsuario)
    }

    @Test
    fun noEncontrado_retornaNull() = runTest {
        val repository = FakeAnimalRepository(animal = null)
        val result = useCase(repository).invoke(9L)

        assertNull(result)
        assertEquals(9L, repository.requestedId)
    }

    @Test
    fun usuarioDistinto_retornaNullSinDatosParciales() = runTest {
        val animal = sampleAnimal()
        val repository = FakeAnimalRepository(animal = animal)
        val result = useCase(
            repository = repository,
            session = FakeSessionRepository(userId = 99L),
        ).invoke(animal.idAnimal)

        assertNull(result)
        assertEquals(animal.idAnimal, repository.requestedId)
        assertEquals("Manchita", animal.nombre)
    }

    @Test
    fun sesionAusente_retornaNullSinConsultar() = runTest {
        val repository = FakeAnimalRepository(animal = sampleAnimal())
        listOf<Long?>(null, 0L, -1L).forEach { userId ->
            repository.requestedId = null
            val result = useCase(
                repository = repository,
                session = FakeSessionRepository(userId = userId),
            ).invoke(4L)

            assertNull(result)
            assertNull(repository.requestedId)
        }
    }

    @Test
    fun excepcion_sePropaga() = runTest {
        val failure = IllegalStateException("db")
        val repository = FakeAnimalRepository(error = failure)
        var thrown: Throwable? = null

        try {
            useCase(repository).invoke(3L)
        } catch (exception: IllegalStateException) {
            thrown = exception
        }

        assertSame(failure, thrown)
    }

    @Test
    fun cancelacion_sePropaga() = runTest {
        val cancellation = CancellationException("cancel")
        val repository = FakeAnimalRepository(error = cancellation)
        var thrown: Throwable? = null

        try {
            useCase(repository).invoke(3L)
        } catch (exception: CancellationException) {
            thrown = exception
        }

        assertSame(cancellation, thrown)
    }

    private fun useCase(
        repository: FakeAnimalRepository,
        session: FakeSessionRepository = FakeSessionRepository(userId = 7L),
    ) = GetAnimalByIdUseCase(repository, session)

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

        override fun getFavoriteAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override suspend fun getAnimalById(id: Long): Animal? {
            requestedId = id
            error?.let { throw it }
            return animal
        }

        override suspend fun insertAnimal(animal: Animal): Long = error("unused")

        override suspend fun updateAnimal(animal: Animal) = error("unused")

        override suspend fun deleteAnimal(id: Long) = error("unused")
    }

    private class FakeSessionRepository(
        private val userId: Long?,
    ) : SessionRepository {
        override fun observeAuthenticatedUserId(): Flow<Long?> = flowOf(userId)

        override suspend fun saveAuthenticatedUserId(userId: Long) = error("unused")

        override suspend fun clearAuthenticatedUserId() = error("unused")
    }
}
