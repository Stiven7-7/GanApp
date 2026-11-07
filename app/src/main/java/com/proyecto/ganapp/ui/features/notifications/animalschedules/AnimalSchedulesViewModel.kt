package com.proyecto.ganapp.ui.features.notifications.animalschedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetAnimalNotificationSchedulesResult
import com.proyecto.ganapp.domain.usecase.notificacion.GetAnimalNotificationSchedulesUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AnimalSchedulesViewModel.Factory::class)
class AnimalSchedulesViewModel @AssistedInject constructor(
    @Assisted private val animalId: Long,
    private val getSchedules: GetAnimalNotificationSchedulesUseCase,
    private val getAnimal: GetAnimalByIdUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(animalId: Long): AnimalSchedulesViewModel
    }

    private val _uiState = MutableStateFlow<AnimalSchedulesContent>(AnimalSchedulesContent.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _uiState.value = AnimalSchedulesContent.Loading
        loadJob = viewModelScope.launch {
            try {
                when (val result = getSchedules(animalId)) {
                    is GetAnimalNotificationSchedulesResult.Success -> showOwned(result)
                    GetAnimalNotificationSchedulesResult.NoAuthenticatedUser ->
                        _uiState.value = AnimalSchedulesContent.Error("No hay una sesión activa.")
                    GetAnimalNotificationSchedulesResult.AnimalNotFound ->
                        _uiState.value = AnimalSchedulesContent.Error("No se encontró el animal.")
                    GetAnimalNotificationSchedulesResult.OwnershipMismatch,
                    GetAnimalNotificationSchedulesResult.InvalidAnimalId,
                    is GetAnimalNotificationSchedulesResult.Failure,
                    -> _uiState.value = AnimalSchedulesContent.Error(AnimalSchedulesErrorMessage)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = AnimalSchedulesContent.Error(AnimalSchedulesErrorMessage)
            }
        }
    }

    private suspend fun showOwned(result: GetAnimalNotificationSchedulesResult.Success) {
        val animal = getAnimal(animalId)
        _uiState.value = if (animal == null) {
            AnimalSchedulesContent.Error(AnimalSchedulesErrorMessage)
        } else if (result.schedules.isEmpty()) {
            AnimalSchedulesContent.Empty(animal)
        } else {
            AnimalSchedulesContent.Success(animal, result.schedules)
        }
    }
}

internal fun leaveAnimalSchedules(onBack: () -> Unit) {
    onBack()
}
