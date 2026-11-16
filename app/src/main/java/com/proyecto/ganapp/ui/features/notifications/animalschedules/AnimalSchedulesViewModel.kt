package com.proyecto.ganapp.ui.features.notifications.animalschedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.DeleteNotificationScheduleResult
import com.proyecto.ganapp.domain.usecase.notificacion.DeleteNotificationScheduleUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetAnimalNotificationSchedulesResult
import com.proyecto.ganapp.domain.usecase.notificacion.GetAnimalNotificationSchedulesUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AnimalSchedulesViewModel.Factory::class)
class AnimalSchedulesViewModel @AssistedInject constructor(
    @Assisted private val animalId: Long,
    private val getSchedules: GetAnimalNotificationSchedulesUseCase,
    private val getAnimal: GetAnimalByIdUseCase,
    private val deleteSchedule: DeleteNotificationScheduleUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(animalId: Long): AnimalSchedulesViewModel
    }

    private val _uiState = MutableStateFlow<AnimalSchedulesContent>(AnimalSchedulesContent.Loading)
    val uiState = _uiState.asStateFlow()
    private val _deletion = MutableStateFlow(AnimalScheduleDeletionUi())
    val deletion = _deletion.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()
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

    fun requestDelete(detail: ProgramacionNotificacionDetalle) {
        if (_deletion.value.isDeleting) return
        _deletion.update { it.copy(target = detail) }
    }

    fun dismissDelete() {
        if (_deletion.value.isDeleting) return
        _deletion.update { it.copy(target = null) }
    }

    fun confirmDelete() {
        val target = _deletion.value.target ?: return
        if (_deletion.value.isDeleting) return
        _deletion.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                when (val result = deleteSchedule(target.programacion.idProgramacion)) {
                    DeleteNotificationScheduleResult.Success -> {
                        _deletion.value = AnimalScheduleDeletionUi()
                        load()
                    }
                    else -> {
                        _deletion.value = AnimalScheduleDeletionUi()
                        _messages.emit(deletionMessage(result))
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _deletion.value = AnimalScheduleDeletionUi()
                _messages.emit("No se pudo eliminar la programación.")
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

private fun deletionMessage(result: DeleteNotificationScheduleResult): String = when (result) {
    DeleteNotificationScheduleResult.Success -> "Programación eliminada."
    DeleteNotificationScheduleResult.NoAuthenticatedUser -> "No hay una sesión activa."
    DeleteNotificationScheduleResult.InvalidScheduleId,
    DeleteNotificationScheduleResult.ScheduleNotFound,
    -> "No se encontró la programación."
    DeleteNotificationScheduleResult.TemplateNotFound -> "No se encontró la plantilla."
    DeleteNotificationScheduleResult.AnimalNotFound -> "No se encontró el animal."
    DeleteNotificationScheduleResult.OwnershipMismatch -> "La programación no pertenece a la sesión."
    is DeleteNotificationScheduleResult.AlarmCancellationFailed ->
        "No se pudo cancelar la alarma. La programación se conservó."
    is DeleteNotificationScheduleResult.PersistenceFailure,
    is DeleteNotificationScheduleResult.Failure,
    -> "No se pudo eliminar la programación."
}
