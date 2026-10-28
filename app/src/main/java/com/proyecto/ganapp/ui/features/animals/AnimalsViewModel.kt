package com.proyecto.ganapp.ui.features.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalsUseCase
import com.proyecto.ganapp.domain.usecase.animal.InsertAnimalUseCase
import com.proyecto.ganapp.domain.usecase.animal.RegisterAnimalRequest
import com.proyecto.ganapp.domain.usecase.animal.RegisterAnimalResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class AnimalsViewModel @Inject constructor(
    private val getAnimalsUseCase: GetAnimalsUseCase,
    private val insertAnimalUseCase: InsertAnimalUseCase,
) : ViewModel() {

    private val _listUiState = MutableStateFlow<AnimalsListUiState>(AnimalsListUiState.Loading)
    val listUiState: StateFlow<AnimalsListUiState> = _listUiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _speciesFilter = MutableStateFlow(AnimalSpeciesFilter.ALL)
    val speciesFilter: StateFlow<AnimalSpeciesFilter> = _speciesFilter

    private var allAnimals: List<Animal> = emptyList()

    private val _registrationUiState = MutableStateFlow<AnimalRegistrationUiState>(
        AnimalRegistrationUiState.Idle,
    )
    val registrationUiState: StateFlow<AnimalRegistrationUiState> = _registrationUiState

    private var loadJob: Job? = null

    fun loadAnimals(userId: Long) {
        loadJob?.cancel()
        _listUiState.value = AnimalsListUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                getAnimalsUseCase(userId).collect { animals ->
                    allAnimals = animals
                    publishVisibleAnimals()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _listUiState.value = AnimalsListUiState.Error(exception)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        republishLoadedAnimals()
    }

    fun updateSpeciesFilter(filter: AnimalSpeciesFilter) {
        _speciesFilter.value = filter
        republishLoadedAnimals()
    }

    private fun republishLoadedAnimals() {
        val current = _listUiState.value
        if (current is AnimalsListUiState.Loading || current is AnimalsListUiState.Error) return
        publishVisibleAnimals()
    }

    private fun publishVisibleAnimals() {
        if (allAnimals.isEmpty()) {
            _listUiState.value = AnimalsListUiState.Empty
            return
        }
        val visibleAnimals = applyFilters(
            animals = allAnimals,
            query = _searchQuery.value,
            speciesFilter = _speciesFilter.value,
        )
        _listUiState.value = if (visibleAnimals.isEmpty()) {
            AnimalsListUiState.NoResults
        } else {
            AnimalsListUiState.Success(visibleAnimals)
        }
    }

    private fun applyFilters(
        animals: List<Animal>,
        query: String,
        speciesFilter: AnimalSpeciesFilter,
    ): List<Animal> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        return animals.filter { animal ->
            val matchesQuery = normalizedQuery.isEmpty() ||
                animal.nombre.lowercase(Locale.ROOT).contains(normalizedQuery)
            val matchesSpecies = when (speciesFilter) {
                AnimalSpeciesFilter.ALL -> true
                AnimalSpeciesFilter.VACA -> animal.especie == EspecieAnimal.VACA
                AnimalSpeciesFilter.CERDO -> animal.especie == EspecieAnimal.CERDO
                AnimalSpeciesFilter.OVEJA -> animal.especie == EspecieAnimal.OVEJA
            }
            matchesQuery && matchesSpecies
        }
    }

    fun registerAnimal(
        especie: EspecieAnimal?,
        nombre: String,
        edad: Int?,
        peso: Float?,
        color: String?,
        fotoUri: String?,
        idUsuario: Long,
    ) {
        val current = _registrationUiState.value
        if (current is AnimalRegistrationUiState.Loading ||
            current is AnimalRegistrationUiState.Success
        ) {
            return
        }
        _registrationUiState.value = AnimalRegistrationUiState.Loading
        viewModelScope.launch {
            val result = insertAnimalUseCase(
                RegisterAnimalRequest(
                    especie = especie,
                    nombre = nombre,
                    edad = edad,
                    peso = peso,
                    color = color,
                    fotoUri = fotoUri,
                    idUsuario = idUsuario,
                ),
            )
            _registrationUiState.value = when (result) {
                is RegisterAnimalResult.Success ->
                    AnimalRegistrationUiState.Success(result.animalId)
                is RegisterAnimalResult.ValidationError ->
                    AnimalRegistrationUiState.ValidationError(result.errors)
                is RegisterAnimalResult.Failure ->
                    AnimalRegistrationUiState.Failure(result.cause)
            }
        }
    }

    fun consumeRegistrationSuccess() {
        if (_registrationUiState.value is AnimalRegistrationUiState.Success) {
            _registrationUiState.value = AnimalRegistrationUiState.Idle
        }
    }
}
