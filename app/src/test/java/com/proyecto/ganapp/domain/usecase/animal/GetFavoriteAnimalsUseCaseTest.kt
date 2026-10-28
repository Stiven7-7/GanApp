package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetFavoriteAnimalsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sesionA_consultaSoloEseUsuario() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(7L, listOf(animal(id = 1L, nombre = "Lola", owner = 7L)))
        val session = FakeSessionRepository(7L)
        val emissions = collect(repository, session)

        assertEquals(listOf("Lola"), emissions.last().map { it.nombre })
        assertEquals(listOf(7L), repository.requestedUserIds)
    }

    @Test
    fun sesionNull_emiteVacioSinConsultar() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        val emissions = collect(repository, FakeSessionRepository(null))

        assertEquals(emptyList<Animal>(), emissions.last())
        assertTrue(repository.requestedUserIds.isEmpty())
    }

    @Test
    fun sesionCero_emiteVacioSinConsultar() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        val emissions = collect(repository, FakeSessionRepository(0L))

        assertEquals(emptyList<Animal>(), emissions.last())
        assertTrue(repository.requestedUserIds.isEmpty())
    }

    @Test
    fun sesionNegativa_emiteVacioSinConsultar() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        val emissions = collect(repository, FakeSessionRepository(-1L))

        assertEquals(emptyList<Animal>(), emissions.last())
        assertTrue(repository.requestedUserIds.isEmpty())
    }

    @Test
    fun repositorioVacio_emiteListaVacia() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(7L, emptyList())
        val emissions = collect(repository, FakeSessionRepository(7L))

        assertEquals(emptyList<Animal>(), emissions.last())
        assertEquals(listOf(7L), repository.requestedUserIds)
    }

    @Test
    fun unFavorito_seEmite() = runTest(mainDispatcherRule.testDispatcher) {
        val lola = animal(id = 4L, nombre = "Lola", owner = 7L)
        val repository = FakeAnimalRepository()
        repository.emit(7L, listOf(lola))
        val emissions = collect(repository, FakeSessionRepository(7L))

        assertEquals(listOf(lola), emissions.last())
    }

    @Test
    fun variosFavoritos_noAplicaLimite() = runTest(mainDispatcherRule.testDispatcher) {
        val animals = (1L..6L).map { id ->
            animal(id = id, nombre = "Animal $id", owner = 7L)
        }
        val repository = FakeAnimalRepository()
        repository.emit(7L, animals)
        val emissions = collect(repository, FakeSessionRepository(7L))

        assertEquals(animals.map { it.idAnimal }, emissions.last().map { it.idAnimal })
        assertEquals(6, emissions.last().size)
    }

    @Test
    fun ordenaPorNombreSinDistinguirMayusculas() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(
            7L,
            listOf(
                animal(id = 1L, nombre = "zeta", owner = 7L),
                animal(id = 2L, nombre = "Ana", owner = 7L),
                animal(id = 3L, nombre = "lola", owner = 7L),
                animal(id = 4L, nombre = "Manolo", owner = 7L),
            ),
        )
        val emissions = collect(repository, FakeSessionRepository(7L))

        assertEquals(listOf("Ana", "lola", "Manolo", "zeta"), emissions.last().map { it.nombre })
    }

    @Test
    fun nombresEquivalentes_desempataPorId() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(
            7L,
            listOf(
                animal(id = 8L, nombre = "Lola", owner = 7L),
                animal(id = 3L, nombre = "lola", owner = 7L),
            ),
        )
        val emissions = collect(repository, FakeSessionRepository(7L))

        assertEquals(listOf(3L, 8L), emissions.last().map { it.idAnimal })
    }

    @Test
    fun nuevaEmisionDelRepositorio_sePublicaOrdenada() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(
            7L,
            listOf(
                animal(id = 2L, nombre = "Nube", owner = 7L),
                animal(id = 1L, nombre = "Lola", owner = 7L),
            ),
        )
        val session = FakeSessionRepository(7L)
        val emissions = mutableListOf<List<Animal>>()
        val job = launch {
            GetFavoriteAnimalsUseCase(repository, session)().collect { emissions += it }
        }
        advanceUntilIdle()

        repository.emit(
            7L,
            listOf(
                animal(id = 4L, nombre = "zeta", owner = 7L),
                animal(id = 3L, nombre = "Ana", owner = 7L),
            ),
        )
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf("Lola", "Nube"), emissions[0].map { it.nombre })
        assertEquals(listOf("Ana", "zeta"), emissions.last().map { it.nombre })
        assertEquals(listOf(7L), repository.requestedUserIds)
    }

    @Test
    fun cambioDeSesion_observaElUsuarioNuevo() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(7L, listOf(animal(id = 1L, nombre = "Lola", owner = 7L)))
        repository.emit(9L, listOf(animal(id = 2L, nombre = "Nube", owner = 9L)))
        val session = FakeSessionRepository(7L)
        val emissions = mutableListOf<List<Animal>>()
        val job = launch {
            GetFavoriteAnimalsUseCase(repository, session)().collect { emissions += it }
        }
        advanceUntilIdle()

        session.userIds.value = 9L
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf("Lola"), emissions.first().map { it.nombre })
        assertEquals(listOf("Nube"), emissions.last().map { it.nombre })
        assertEquals(listOf(7L, 9L), repository.requestedUserIds)
    }

    @Test
    fun emisionTardiaDelUsuarioAnterior_noContaminaElActual() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(7L, listOf(animal(id = 1L, nombre = "Lola", owner = 7L)))
        repository.emit(9L, listOf(animal(id = 2L, nombre = "Nube", owner = 9L)))
        val session = FakeSessionRepository(7L)
        val emissions = mutableListOf<List<Animal>>()
        val job = launch {
            GetFavoriteAnimalsUseCase(repository, session)().collect { emissions += it }
        }
        advanceUntilIdle()

        session.userIds.value = 9L
        advanceUntilIdle()
        val sizeAfterSwitch = emissions.size

        repository.emit(7L, listOf(animal(id = 3L, nombre = "Manolo", owner = 7L)))
        advanceUntilIdle()
        job.cancel()

        assertEquals(sizeAfterSwitch, emissions.size)
        assertEquals(listOf("Nube"), emissions.last().map { it.nombre })
        assertEquals(listOf(7L, 9L), repository.requestedUserIds)
    }

    @Test
    fun logoutReactivo_emiteVacioSinConsultarIdInvalido() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(7L, listOf(animal(id = 1L, nombre = "Lola", owner = 7L)))
        val session = FakeSessionRepository(7L)
        val emissions = mutableListOf<List<Animal>>()
        val job = launch {
            GetFavoriteAnimalsUseCase(repository, session)().collect { emissions += it }
        }
        advanceUntilIdle()

        session.userIds.value = null
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf("Lola"), emissions.first().map { it.nombre })
        assertEquals(emptyList<Animal>(), emissions.last())
        assertEquals(listOf(7L), repository.requestedUserIds)
    }

    @Test
    fun loginDespuesDeLogout_noMezclaFlujos() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAnimalRepository()
        repository.emit(7L, listOf(animal(id = 1L, nombre = "Lola", owner = 7L)))
        repository.emit(9L, listOf(animal(id = 2L, nombre = "Nube", owner = 9L)))
        val session = FakeSessionRepository(7L)
        val emissions = mutableListOf<List<Animal>>()
        val job = launch {
            GetFavoriteAnimalsUseCase(repository, session)().collect { emissions += it }
        }
        advanceUntilIdle()

        session.userIds.value = null
        advanceUntilIdle()
        session.userIds.value = 9L
        advanceUntilIdle()
        job.cancel()

        assertEquals(
            listOf(listOf("Lola"), emptyList(), listOf("Nube")),
            emissions.map { animals -> animals.map { it.nombre } },
        )
        assertEquals(listOf(7L, 9L), repository.requestedUserIds)
    }

    @Test
    fun errorDeSesion_sePropaga() = runTest(mainDispatcherRule.testDispatcher) {
        val failure = IllegalStateException("session")
        val session = FakeSessionRepository(7L)
        session.failure = failure
        var thrown: Throwable? = null

        val job = launch {
            try {
                GetFavoriteAnimalsUseCase(FakeAnimalRepository(), session)().collect { }
            } catch (exception: Throwable) {
                thrown = exception
            }
        }
        advanceUntilIdle()
        job.cancel()

        assertTrue(thrown is IllegalStateException)
        assertEquals("session", thrown?.message)
    }

    @Test
    fun errorDeRepositorio_sePropaga() = runTest(mainDispatcherRule.testDispatcher) {
        val failure = IllegalStateException("db")
        val repository = FakeAnimalRepository()
        repository.failure = failure
        var thrown: Throwable? = null
        var emitted = false

        val job = launch {
            try {
                GetFavoriteAnimalsUseCase(repository, FakeSessionRepository(7L))().collect {
                    emitted = true
                }
            } catch (exception: Throwable) {
                thrown = exception
            }
        }
        advanceUntilIdle()
        job.cancel()

        assertTrue(thrown is IllegalStateException)
        assertEquals("db", thrown?.message)
        assertEquals(false, emitted)
    }

    private fun animal(id: Long, nombre: String, owner: Long) = Animal(
        idAnimal = id,
        especie = EspecieAnimal.VACA,
        nombre = nombre,
        raza = "Holstein",
        edad = 2,
        peso = 80f,
        color = "Blanco",
        fotoUri = null,
        idUsuario = owner,
    )

    private suspend fun TestScope.collect(
        repository: FakeAnimalRepository,
        session: FakeSessionRepository,
    ): List<List<Animal>> {
        val emissions = mutableListOf<List<Animal>>()
        val job = launch {
            GetFavoriteAnimalsUseCase(repository, session)().collect { emissions += it }
        }
        advanceUntilIdle()
        job.cancel()
        return emissions
    }

    private class FakeAnimalRepository : AnimalRepository {
        val requestedUserIds = mutableListOf<Long>()
        val flows = mutableMapOf<Long, MutableStateFlow<List<Animal>>>()
        var failure: Throwable? = null

        fun emit(userId: Long, animals: List<Animal>) {
            flows.getOrPut(userId) { MutableStateFlow(emptyList()) }.value = animals
        }

        override fun getFavoriteAnimalsByUser(userId: Long): Flow<List<Animal>> {
            requestedUserIds += userId
            failure?.let { return flow { throw it } }
            return flows.getOrPut(userId) { MutableStateFlow(emptyList()) }
        }

        override fun getAllAnimals(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override suspend fun getAnimalById(id: Long): Animal? = null

        override suspend fun insertAnimal(animal: Animal): Long = error("unused")

        override suspend fun updateAnimal(animal: Animal): Boolean = error("unused")

        override suspend fun deleteAnimal(id: Long) = error("unused")
    }

    private class FakeSessionRepository(
        initialUserId: Long?,
    ) : SessionRepository {
        val userIds = MutableStateFlow(initialUserId)
        var failure: Throwable? = null

        override fun observeAuthenticatedUserId(): Flow<Long?> {
            failure?.let { error -> return flow { throw error } }
            return userIds
        }

        override suspend fun saveAuthenticatedUserId(userId: Long) = error("unused")

        override suspend fun clearAuthenticatedUserId() = error("unused")
    }
}
