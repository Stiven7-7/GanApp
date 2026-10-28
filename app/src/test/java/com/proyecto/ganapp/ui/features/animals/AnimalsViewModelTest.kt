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
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Test
    fun queryVacia_muestraTodos() = runTest(mainDispatcherRule.testDispatcher) {
        val animals = searchFixture()
        repository.animals = animals
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("")

        assertEquals(animals, visibleAnimals())
    }

    @Test
    fun querySoloEspacios_noFiltraPorNombre() = runTest(mainDispatcherRule.testDispatcher) {
        val animals = searchFixture()
        repository.animals = animals
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("   ")

        assertEquals(animals, visibleAnimals())
    }

    @Test
    fun queryConEspacios_seRecortaAntesDeComparar() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = searchFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("  lo  ")

        assertEquals(listOf("Lola", "Manolo"), visibleNames())
    }

    @Test
    fun queryIgnoraMayusculas() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = listOf(
            sampleAnimal(id = 1L, especie = EspecieAnimal.VACA, nombre = "Lola"),
        )
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("LOLA")

        assertEquals(listOf("Lola"), visibleNames())
    }

    @Test
    fun queryContains_incluyeCoincidenciasParciales() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = searchFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("lo")

        assertEquals(listOf("Lola", "Manolo"), visibleNames())
    }

    @Test
    fun querySinCoincidencias_publicaNoResults() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = searchFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("zzz")

        assertEquals(AnimalsListUiState.NoResults, viewModel.listUiState.value)
    }

    @Test
    fun limpiarQuery_restauraLaListaCompleta() = runTest(mainDispatcherRule.testDispatcher) {
        val animals = searchFixture()
        repository.animals = animals
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("lo")
        assertEquals(listOf("Lola", "Manolo"), visibleNames())

        viewModel.updateSearchQuery("")

        assertEquals(animals, visibleAnimals())
    }

    @Test
    fun speciesAll_incluyeLegacy() = runTest(mainDispatcherRule.testDispatcher) {
        val animals = speciesFixture()
        repository.animals = animals
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.ALL)

        assertEquals(animals, visibleAnimals())
    }

    @Test
    fun speciesVaca_soloVacas() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = speciesFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)

        assertEquals(listOf(EspecieAnimal.VACA), visibleSpecies())
    }

    @Test
    fun speciesCerdo_soloCerdos() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = speciesFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.CERDO)

        assertEquals(listOf(EspecieAnimal.CERDO), visibleSpecies())
    }

    @Test
    fun speciesOveja_soloOvejas() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = speciesFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.OVEJA)

        assertEquals(listOf(EspecieAnimal.OVEJA), visibleSpecies())
    }

    @Test
    fun searchYSpecies_seAcumulan() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = combinedFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("lo")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)

        assertEquals(
            listOf("Lola" to EspecieAnimal.VACA, "Manolo" to EspecieAnimal.VACA),
            visibleIdentity(),
        )
    }

    @Test
    fun cambiarSpecies_conservaQuery() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = combinedFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("lo")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.ALL)
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)

        assertEquals("lo", viewModel.searchQuery.value)
        assertEquals(
            listOf("Lola" to EspecieAnimal.VACA, "Manolo" to EspecieAnimal.VACA),
            visibleIdentity(),
        )
    }

    @Test
    fun limpiarSearch_conservaSpecies() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = combinedFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)
        viewModel.updateSearchQuery("lo")
        viewModel.updateSearchQuery("")

        assertEquals(AnimalSpeciesFilter.VACA, viewModel.speciesFilter.value)
        assertEquals(listOf(EspecieAnimal.VACA, EspecieAnimal.VACA), visibleSpecies())
    }

    @Test
    fun limpiarAmbosFiltros_restauraColeccionCompleta() = runTest(mainDispatcherRule.testDispatcher) {
        val animals = combinedFixture()
        repository.animals = animals
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("lo")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)
        viewModel.updateSearchQuery("")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.ALL)

        assertEquals(animals, visibleAnimals())
    }

    @Test
    fun filtrosNoReemplazanLaFuente() = runTest(mainDispatcherRule.testDispatcher) {
        val source = listOf(
            sampleAnimal(id = 1L, especie = EspecieAnimal.VACA, nombre = "Lola"),
            sampleAnimal(id = 2L, especie = EspecieAnimal.CERDO, nombre = "Rosado"),
            sampleAnimal(id = 3L, especie = EspecieAnimal.OVEJA, nombre = "Nube"),
        )
        repository.animals = source
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("Lola")
        assertEquals(listOf("Lola"), visibleNames())

        viewModel.updateSearchQuery("")
        assertEquals(source, visibleAnimals())

        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.OVEJA)
        assertEquals(listOf("Nube"), visibleNames())

        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.ALL)
        assertEquals(source, visibleAnimals())
        assertEquals(1, repository.getAllAnimalsCalls)
    }

    @Test
    fun cambiarFiltros_noVuelveAConsultarAnimales() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = combinedFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()
        assertEquals(1, repository.getAllAnimalsCalls)

        viewModel.updateSearchQuery("lo")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)
        viewModel.updateSearchQuery("")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.ALL)
        advanceUntilIdle()

        assertEquals(1, repository.getAllAnimalsCalls)
    }

    @Test
    fun nuevaEmision_conservaFiltrosActivos() = runTest(mainDispatcherRule.testDispatcher) {
        repository.animals = combinedFixture()
        viewModel.loadAnimals(7L)
        advanceUntilIdle()
        viewModel.updateSearchQuery("lo")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)

        repository.animals = listOf(
            sampleAnimal(id = 10L, especie = EspecieAnimal.VACA, nombre = "Lola"),
            sampleAnimal(id = 11L, especie = EspecieAnimal.CERDO, nombre = "Lola"),
            sampleAnimal(id = 12L, especie = EspecieAnimal.OVEJA, nombre = "Nube"),
        )
        advanceUntilIdle()

        assertEquals("lo", viewModel.searchQuery.value)
        assertEquals(AnimalSpeciesFilter.VACA, viewModel.speciesFilter.value)
        assertEquals(listOf("Lola" to EspecieAnimal.VACA), visibleIdentity())
        assertEquals(1, repository.getAllAnimalsCalls)
    }

    @Test
    fun favoritoNoAlteraElFiltro() = runTest(mainDispatcherRule.testDispatcher) {
        val favorite = sampleAnimal(
            id = 1L,
            especie = EspecieAnimal.VACA,
            nombre = "Lola",
            esFavorito = true,
        )
        val regular = sampleAnimal(
            id = 2L,
            especie = EspecieAnimal.VACA,
            nombre = "Lola",
            esFavorito = false,
        )
        repository.animals = listOf(favorite, regular)
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("lola")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)
        assertEquals(listOf(true, false), visibleAnimals().map { it.esFavorito })

        viewModel.updateSearchQuery("nube")
        assertEquals(AnimalsListUiState.NoResults, viewModel.listUiState.value)

        viewModel.updateSearchQuery("lola")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.CERDO)
        assertEquals(AnimalsListUiState.NoResults, viewModel.listUiState.value)
    }

    @Test
    fun listaFuenteVaciaConFiltros_sigueEnEmpty() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.loadAnimals(7L)
        advanceUntilIdle()

        viewModel.updateSearchQuery("lo")
        viewModel.updateSpeciesFilter(AnimalSpeciesFilter.VACA)

        assertEquals(AnimalsListUiState.Empty, viewModel.listUiState.value)
    }

    private fun searchFixture() = listOf(
        sampleAnimal(id = 1L, especie = EspecieAnimal.VACA, nombre = "Lola"),
        sampleAnimal(id = 2L, especie = EspecieAnimal.CERDO, nombre = "Manolo"),
        sampleAnimal(id = 3L, especie = EspecieAnimal.OVEJA, nombre = "Nube"),
    )

    private fun speciesFixture() = listOf(
        sampleAnimal(id = 1L, especie = EspecieAnimal.VACA, nombre = "Lola"),
        sampleAnimal(id = 2L, especie = EspecieAnimal.CERDO, nombre = "Rosado"),
        sampleAnimal(id = 3L, especie = EspecieAnimal.OVEJA, nombre = "Nube"),
        sampleAnimal(id = 4L, especie = EspecieAnimal.SIN_ESPECIFICAR, nombre = "Histórico"),
    )

    private fun combinedFixture() = listOf(
        sampleAnimal(id = 1L, especie = EspecieAnimal.VACA, nombre = "Lola"),
        sampleAnimal(id = 2L, especie = EspecieAnimal.VACA, nombre = "Manolo"),
        sampleAnimal(id = 3L, especie = EspecieAnimal.CERDO, nombre = "Lola"),
        sampleAnimal(id = 4L, especie = EspecieAnimal.OVEJA, nombre = "Nube"),
        sampleAnimal(id = 5L, especie = EspecieAnimal.SIN_ESPECIFICAR, nombre = "Histórico"),
    )

    private fun visibleAnimals(): List<Animal> {
        val state = viewModel.listUiState.value
        assertTrue(state is AnimalsListUiState.Success)
        return (state as AnimalsListUiState.Success).animals
    }

    private fun visibleNames() = visibleAnimals().map { it.nombre }

    private fun visibleSpecies() = visibleAnimals().map { it.especie }

    private fun visibleIdentity() = visibleAnimals().map { it.nombre to it.especie }

    private fun sampleAnimal(
        id: Long,
        especie: EspecieAnimal,
        nombre: String,
        esFavorito: Boolean = false,
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
        esFavorito = esFavorito,
    )

    private class FakeAnimalRepository : AnimalRepository {
        var insertCalls = 0
        var inserted: Animal? = null
        var insertGate: CompletableDeferred<Long>? = null
        var getAllAnimalsCalls = 0
        var animalsFlow: Flow<List<Animal>>? = null
        var requestedUserId: Long? = null
        private val animalsState = MutableStateFlow<List<Animal>>(emptyList())
        var animals: List<Animal>
            get() = animalsState.value
            set(value) {
                animalsState.value = value
            }

        override fun getAllAnimals(userId: Long): Flow<List<Animal>> {
            requestedUserId = userId
            getAllAnimalsCalls += 1
            return animalsFlow ?: animalsState
        }

        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = flowOf(emptyList())

        override suspend fun getAnimalById(id: Long): Animal? = null

        override suspend fun insertAnimal(animal: Animal): Long {
            insertCalls += 1
            inserted = animal
            return insertGate?.await() ?: 1L
        }

        override suspend fun updateAnimal(animal: Animal): Boolean {
            error("Unexpected call to updateAnimal")
        }

        override suspend fun deleteAnimal(id: Long) {
            error("Unexpected call to deleteAnimal")
        }
    }
}
