package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAnimalRepository
    private lateinit var session: FakeSessionRepository
    private lateinit var viewModel: AnimalDetailViewModel

    @Before
    fun setUp() {
        repository = FakeAnimalRepository()
        session = FakeSessionRepository(userId = 7L)
        viewModel = AnimalDetailViewModel(
            getAnimalByIdUseCase = GetAnimalByIdUseCase(repository, session),
        )
    }

    @Test
    fun alCrear_empiezaEnLoading() {
        assertEquals(AnimalDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun animalEncontrado_publicaSuccessIntacta() = runTest(mainDispatcherRule.testDispatcher) {
        val animal = sampleAnimal()
        repository.animal = animal

        viewModel.loadAnimal(animal.idAnimal)
        advanceUntilIdle()

        assertEquals(AnimalDetailUiState.Success(animal), viewModel.uiState.value)
        assertEquals(animal.idAnimal, repository.requestedId)
    }

    @Test
    fun animalAjeno_publicaNotFound() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animal = sampleAnimal()
        session.userId = 99L

        viewModel.loadAnimal(4L)
        advanceUntilIdle()

        assertEquals(AnimalDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun animalAusente_publicaNotFound() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animal = null

        viewModel.loadAnimal(99L)
        advanceUntilIdle()

        assertEquals(AnimalDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun excepcion_publicaError() = runTest(mainDispatcherRule.testDispatcher) {
        val failure = IllegalStateException("db")
        repository.error = failure

        viewModel.loadAnimal(4L)
        advanceUntilIdle()

        assertEquals(AnimalDetailUiState.Error(failure), viewModel.uiState.value)
    }

    @Test
    fun sinEspecificar_llegaIntacto() = runTest(mainDispatcherRule.testDispatcher) {
        val animal = sampleAnimal(especie = EspecieAnimal.SIN_ESPECIFICAR, nombre = "Histórico")
        repository.animal = animal

        viewModel.loadAnimal(animal.idAnimal)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnimalDetailUiState.Success(animal), state)
        assertEquals(EspecieAnimal.SIN_ESPECIFICAR, (state as AnimalDetailUiState.Success).animal.especie)
    }

    @Test
    fun nullables_siguenEnSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val animal = sampleAnimal(edad = null, peso = null, color = null, fotoUri = null)
        repository.animal = animal

        viewModel.loadAnimal(animal.idAnimal)
        advanceUntilIdle()

        assertEquals(AnimalDetailUiState.Success(animal), viewModel.uiState.value)
    }

    private fun sampleAnimal(
        especie: EspecieAnimal = EspecieAnimal.VACA,
        nombre: String = "Lola",
        edad: Int? = 0,
        peso: Float? = 0f,
        color: String? = "Blanco",
        fotoUri: String? = "content://ganapp/lola",
    ) = Animal(
        idAnimal = 4L,
        especie = especie,
        nombre = nombre,
        raza = "Holstein",
        edad = edad,
        peso = peso,
        color = color,
        fotoUri = fotoUri,
        idUsuario = 7L,
    )

    private class FakeAnimalRepository : AnimalRepository {
        var animal: Animal? = null
        var error: Throwable? = null
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

    private class FakeSessionRepository(
        userId: Long?,
    ) : SessionRepository {
        var userId: Long? = userId

        override fun observeAuthenticatedUserId(): Flow<Long?> = flowOf(userId)

        override suspend fun saveAuthenticatedUserId(userId: Long) = error("unused")

        override suspend fun clearAuthenticatedUserId() = error("unused")
    }
}
