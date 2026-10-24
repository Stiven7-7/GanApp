package com.proyecto.ganapp.ui.features.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class AnimalsViewModel @Inject constructor(
    private val getAnimalsUseCase: GetAnimalsUseCase,
    private val insertAnimalUseCase: InsertAnimalUseCase,
) : ViewModel() {

    private val _listUiState = MutableStateFlow<AnimalsListUiState>(AnimalsListUiState.Loading)
    val listUiState: StateFlow<AnimalsListUiState> = _listUiState

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
                    _listUiState.value = if (animals.isEmpty()) {
                        AnimalsListUiState.Empty
                    } else {
                        AnimalsListUiState.Success(animals)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _listUiState.value = AnimalsListUiState.Error(exception)
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
