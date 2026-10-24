package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalsUseCase
import com.proyecto.ganapp.domain.usecase.animal.InsertAnimalUseCase
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAnimalRepository
    private lateinit var viewModel: AnimalsViewModel

    @Before
    fun setUp() {
        repository = FakeAnimalRepository()
        viewModel = AnimalsViewModel(
            getAnimalsUseCase = GetAnimalsUseCase(repository),
            insertAnimalUseCase = InsertAnimalUseCase(repository),
        )
    }

    @Test
    fun registroPendiente_segundaLlamadaNoEjecutaOtroInsert() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.insertGate = CompletableDeferred()

            viewModel.registerAnimal(
                especie = EspecieAnimal.VACA,
                nombre = "Lola",
                edad = null,
                peso = null,
                color = null,
                fotoUri = "content://ganapp/lola",
                idUsuario = 7L,
            )
            advanceUntilIdle()

            assertTrue(viewModel.registrationUiState.value is AnimalRegistrationUiState.Loading)
            assertEquals(1, repository.insertCalls)
            assertEquals("content://ganapp/lola", repository.inserted?.fotoUri)

            viewModel.registerAnimal(
                especie = EspecieAnimal.CERDO,
                nombre = "Rosado",
                edad = 2,
                peso = 80f,
                color = "Blanco",
                fotoUri = "content://ganapp/rosado",
                idUsuario = 7L,
            )

            assertEquals(1, repository.insertCalls)
            assertEquals(EspecieAnimal.VACA, repository.inserted?.especie)
            assertEquals("Lola", repository.inserted?.nombre)
            assertEquals("content://ganapp/lola", repository.inserted?.fotoUri)
            assertTrue(viewModel.registrationUiState.value is AnimalRegistrationUiState.Loading)

            repository.insertGate!!.complete(42L)
            advanceUntilIdle()

            assertEquals(1, repository.insertCalls)
            val success = viewModel.registrationUiState.value
            assertTrue(success is AnimalRegistrationUiState.Success)
            assertEquals(42L, (success as AnimalRegistrationUiState.Success).animalId)
        }

    @Test
    fun alCrear_elListadoEmpiezaEnLoading() {
        assertEquals(AnimalsListUiState.Loading, viewModel.listUiState.value)
    }

    @Test
    fun listaVacia_publicaEmpty() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.loadAnimals(15L)
        advanceUntilIdle()

        assertEquals(AnimalsListUiState.Empty, viewModel.listUiState.value)
        assertEquals(15L, repository.requestedUserId)
    }

    @Test
    fun listaConAnimales_publicaSuccessSinFiltrar() = runTest(mainDispatcherRule.testDispatcher) {
        val animals = listOf(
            sampleAnimal(id = 1L, especie = EspecieAnimal.VACA, nombre = "Lola"),
            sampleAnimal(id = 2L, especie = EspecieAnimal.CERDO, nombre = "Manchita"),
            sampleAnimal(id = 3L, especie = EspecieAnimal.OVEJA, nombre = "Nube"),
            sampleAnimal(id = 4L, especie = EspecieAnimal.SIN_ESPECIFICAR, nombre = "Histórico"),
        )
        repository.animals = animals

        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        val state = viewModel.listUiState.value
        assertTrue(state is AnimalsListUiState.Success)
        assertEquals(animals, (state as AnimalsListUiState.Success).animals)
        assertEquals(7L, repository.requestedUserId)
    }

    @Test
    fun flujoFallido_publicaError() = runTest(mainDispatcherRule.testDispatcher) {
        val failure = IllegalStateException("db")
        repository.animalsFlow = flow { throw failure }

        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        assertEquals(AnimalsListUiState.Error(failure), viewModel.listUiState.value)
    }

    private fun sampleAnimal(
        id: Long,
        especie: EspecieAnimal,
        nombre: String,
    ) = Animal(
        idAnimal = id,
        especie = especie,
        nombre = nombre,
        raza = "SIN_ESPECIFICAR",
        edad = 2,
        peso = 80f,
        color = "Blanco",
        fotoUri = null,
        idUsuario = 7L,
    )

    private class FakeAnimalRepository : AnimalRepository {
        var insertCalls = 0
        var inserted: Animal? = null
        var insertGate: CompletableDeferred<Long>? = null
        var animals: List<Animal> = emptyList()
        var animalsFlow: Flow<List<Animal>>? = null
        var requestedUserId: Long? = null

        override fun getAllAnimals(userId: Long): Flow<List<Animal>> {
            requestedUserId = userId
            return animalsFlow ?: flowOf(animals)
        }

        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override suspend fun getAnimalById(id: Long): Animal? = null

        override suspend fun insertAnimal(animal: Animal): Long {
            insertCalls += 1
            inserted = animal
            return insertGate?.await() ?: 1L
        }

        override suspend fun updateAnimal(animal: Animal) {
            error("Unexpected call to updateAnimal")
        }

        override suspend fun deleteAnimal(id: Long) {
            error("Unexpected call to deleteAnimal")
        }
    }
}
