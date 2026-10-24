package com.proyecto.ganapp.ui.features.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class AnimalDetailViewModel @Inject constructor(
    private val getAnimalByIdUseCase: GetAnimalByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnimalDetailUiState>(AnimalDetailUiState.Loading)
    val uiState: StateFlow<AnimalDetailUiState> = _uiState

    private var loadJob: Job? = null

    fun loadAnimal(animalId: Long) {
        loadJob?.cancel()
        _uiState.value = AnimalDetailUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val animal = getAnimalByIdUseCase(animalId)
                _uiState.value = if (animal == null) {
                    AnimalDetailUiState.NotFound
                } else {
                    AnimalDetailUiState.Success(animal)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = AnimalDetailUiState.Error(exception)
            }
        }
    }
}
