package com.proyecto.ganapp.ui.features.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalRequest
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalResult
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class AnimalEditViewModel @Inject constructor(
    private val getAnimalByIdUseCase: GetAnimalByIdUseCase,
    private val updateAnimalUseCase: UpdateAnimalUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnimalEditUiState>(AnimalEditUiState.Loading)
    val uiState: StateFlow<AnimalEditUiState> = _uiState

    private var loadJob: Job? = null

    fun loadAnimal(animalId: Long) {
        loadJob?.cancel()
        _uiState.value = AnimalEditUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val animal = getAnimalByIdUseCase(animalId)
                _uiState.value = if (animal == null) {
                    AnimalEditUiState.NotFound
                } else {
                    AnimalEditUiState.Ready(animal)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = AnimalEditUiState.Error(exception)
            }
        }
    }

    fun saveAnimal(
        especie: EspecieAnimal?,
        nombre: String,
        edad: Int?,
        peso: Float?,
        color: String?,
        fotoUri: String?,
        esFavorito: Boolean,
    ) {
        val current = _uiState.value
        if (current is AnimalEditUiState.Saving) return
        val animal = current.editableAnimal() ?: return
        _uiState.value = AnimalEditUiState.Saving(animal)
        viewModelScope.launch {
            val result = updateAnimalUseCase(
                UpdateAnimalRequest(
                    animalId = animal.idAnimal,
                    especie = especie,
                    nombre = nombre,
                    edad = edad,
                    peso = peso,
                    color = color,
                    fotoUri = fotoUri,
                    esFavorito = esFavorito,
                ),
            )
            _uiState.value = when (result) {
                is UpdateAnimalResult.Success -> AnimalEditUiState.Success(result.animal)
                is UpdateAnimalResult.ValidationError -> AnimalEditUiState.ValidationError(
                    animal = animal,
                    errors = result.errors,
                )
                UpdateAnimalResult.NotFound -> AnimalEditUiState.NotFound
                is UpdateAnimalResult.Failure -> AnimalEditUiState.Error(
                    cause = result.cause,
                    animal = animal,
                )
            }
        }
    }

    private fun AnimalEditUiState.editableAnimal(): Animal? {
        return when (this) {
            is AnimalEditUiState.Ready -> animal
            is AnimalEditUiState.ValidationError -> animal
            is AnimalEditUiState.Error -> animal
            else -> null
        }
    }
}
