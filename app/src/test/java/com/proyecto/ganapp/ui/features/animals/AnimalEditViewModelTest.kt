package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalUseCase
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalValidationError
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAnimalRepository
    private lateinit var viewModel: AnimalEditViewModel

    @Before
    fun setUp() {
        repository = FakeAnimalRepository()
        viewModel = AnimalEditViewModel(
            getAnimalByIdUseCase = GetAnimalByIdUseCase(repository),
            updateAnimalUseCase = UpdateAnimalUseCase(repository),
        )
    }

    @Test
    fun alCrear_empiezaEnLoading() {
        assertEquals(AnimalEditUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun cargaExistente_publicaReadyConLosDatos() = runTest(mainDispatcherRule.testDispatcher) {
        val animal = sample(edad = 0, peso = 0f, color = null, fotoUri = "content://ganapp/lola")
        repository.stored = animal

        viewModel.loadAnimal(animal.idAnimal)
        advanceUntilIdle()

        assertEquals(AnimalEditUiState.Ready(animal), viewModel.uiState.value)
    }

    @Test
    fun cargaInexistente_publicaNotFound() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.loadAnimal(99L)
        advanceUntilIdle()

        assertEquals(AnimalEditUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun cargaConExcepcion_publicaError() = runTest(mainDispatcherRule.testDispatcher) {
        val failure = IllegalStateException("db")
        repository.getError = failure

        viewModel.loadAnimal(4L)
        advanceUntilIdle()

        assertEquals(AnimalEditUiState.Error(failure), viewModel.uiState.value)
    }

    @Test
    fun guardadoInvalido_vuelveAValidationError() = runTest(mainDispatcherRule.testDispatcher) {
        val animal = sample(especie = EspecieAnimal.SIN_ESPECIFICAR)
        repository.stored = animal
        viewModel.loadAnimal(animal.idAnimal)
        advanceUntilIdle()

        viewModel.saveAnimal(
            especie = null,
            nombre = "Histórico",
            edad = null,
            peso = null,
            color = null,
            fotoUri = null,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as AnimalEditUiState.ValidationError
        assertEquals(animal, state.animal)
        assertTrue(state.errors.contains(UpdateAnimalValidationError.SPECIES_REQUIRED))
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun historicoConVaca_publicaSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val animal = sample(especie = EspecieAnimal.SIN_ESPECIFICAR, raza = "SIN_ESPECIFICAR")
        repository.stored = animal
        viewModel.loadAnimal(animal.idAnimal)
        advanceUntilIdle()

        viewModel.saveAnimal(
            especie = EspecieAnimal.VACA,
            nombre = "Histórico",
            edad = null,
            peso = null,
            color = null,
            fotoUri = null,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as AnimalEditUiState.Success
        assertEquals(EspecieAnimal.VACA, state.animal.especie)
        assertEquals("SIN_ESPECIFICAR", state.animal.raza)
        assertEquals(animal.idAnimal, state.animal.idAnimal)
        assertEquals(animal.idUsuario, state.animal.idUsuario)
    }

    @Test
    fun falloAlGuardar_conservaElAnimalBase() = runTest(mainDispatcherRule.testDispatcher) {
        val animal = sample()
        val failure = IllegalStateException("db")
        repository.stored = animal
        repository.updateError = failure
        viewModel.loadAnimal(animal.idAnimal)
        advanceUntilIdle()

        viewModel.saveAnimal(
            especie = EspecieAnimal.CERDO,
            nombre = "Lola",
            edad = 2,
            peso = 80f,
            color = "Blanco",
            fotoUri = "content://ganapp/lola",
        )
        advanceUntilIdle()

        assertEquals(AnimalEditUiState.Error(failure, animal), viewModel.uiState.value)
    }

    @Test
    fun segundoGuardadoDuranteSaving_noRepiteElUpdate() = runTest(mainDispatcherRule.testDispatcher) {
        val animal = sample()
        repository.stored = animal
        repository.updateGate = CompletableDeferred()
        viewModel.loadAnimal(animal.idAnimal)
        advanceUntilIdle()

        viewModel.saveAnimal(
            especie = EspecieAnimal.VACA,
            nombre = "Lola",
            edad = 2,
            peso = 80f,
            color = "Blanco",
            fotoUri = "content://ganapp/lola",
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AnimalEditUiState.Saving)
        assertEquals(1, repository.updateCalls)

        viewModel.saveAnimal(
            especie = EspecieAnimal.CERDO,
            nombre = "Otra",
            edad = 9,
            peso = 10f,
            color = "Negro",
            fotoUri = "content://otra",
        )

        assertEquals(1, repository.updateCalls)
        assertEquals(EspecieAnimal.VACA, repository.updated?.especie)
        repository.updateGate!!.complete(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AnimalEditUiState.Success)
        assertEquals(1, repository.updateCalls)
    }

    private fun sample(
        especie: EspecieAnimal = EspecieAnimal.VACA,
        raza: String = "Holstein",
        edad: Int? = 2,
        peso: Float? = 80f,
        color: String? = "Blanco",
        fotoUri: String? = "content://ganapp/lola",
    ) = Animal(
        idAnimal = 4L,
        especie = especie,
        nombre = "Lola",
        raza = raza,
        edad = edad,
        peso = peso,
        color = color,
        fotoUri = fotoUri,
        idUsuario = 7L,
    )

    private class FakeAnimalRepository : AnimalRepository {
        var stored: Animal? = null
        var getError: Throwable? = null
        var updateError: Throwable? = null
        var updateGate: CompletableDeferred<Boolean>? = null
        var updateCalls = 0
        var updated: Animal? = null

        override fun getAllAnimals(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override suspend fun getAnimalById(id: Long): Animal? {
            getError?.let { throw it }
            return stored?.takeIf { it.idAnimal == id }
        }

        override suspend fun insertAnimal(animal: Animal): Long = error("unused")

        override suspend fun updateAnimal(animal: Animal): Boolean {
            updateCalls += 1
            updated = animal
            updateError?.let { throw it }
            return updateGate?.await() ?: true
        }

        override suspend fun deleteAnimal(id: Long) = error("unused")
    }
}
