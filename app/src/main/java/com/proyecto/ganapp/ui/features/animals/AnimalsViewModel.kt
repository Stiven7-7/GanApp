package com.proyecto.ganapp.ui.features.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalsUseCase
import com.proyecto.ganapp.domain.usecase.animal.InsertAnimalUseCase
import com.proyecto.ganapp.domain.usecase.animal.RegisterAnimalRequest
import com.proyecto.ganapp.domain.usecase.animal.RegisterAnimalResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimalsViewModel @Inject constructor(
    private val getAnimalsUseCase: GetAnimalsUseCase,
    private val insertAnimalUseCase: InsertAnimalUseCase,
    private val animalRepository: AnimalRepository
) : ViewModel() {

    private val _animals = MutableStateFlow<List<Animal>>(emptyList())
    val animals: StateFlow<List<Animal>> = _animals

    private val _registrationUiState = MutableStateFlow<AnimalRegistrationUiState>(
        AnimalRegistrationUiState.Idle,
    )
    val registrationUiState: StateFlow<AnimalRegistrationUiState> = _registrationUiState

    fun loadAnimals(userId: Long) {
        viewModelScope.launch {
            animalRepository.getAnimalsByUser(userId).collect { list ->
                _animals.value = list
            }
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
