package com.proyecto.ganapp.ui.features.notifications.scheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalByIdUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.CreateAnimalNotificationScheduleRequest
import com.proyecto.ganapp.domain.usecase.notificacion.CreateAnimalNotificationScheduleResult
import com.proyecto.ganapp.domain.usecase.notificacion.CreateAnimalNotificationScheduleUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationTemplatesUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.ScheduleValidationError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Preparado sin HiltViewModel ni binding: el animal llega del flujo y la sesión queda en los use cases. */
class AnimalNotificationScheduleViewModel(
    private val animalId: Long,
    private val getAnimal: GetAnimalByIdUseCase,
    private val getTemplates: GetNotificationTemplatesUseCase,
    private val createSchedule: CreateAnimalNotificationScheduleUseCase,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnimalNotificationScheduleUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AnimalScheduleEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(load = AnimalScheduleLoad.Loading) }
        loadJob = viewModelScope.launch {
            try {
                val animal = getAnimal(animalId)
                if (animal == null) {
                    _uiState.update { it.copy(load = AnimalScheduleLoad.Error("No se encontró el animal.")) }
                    return@launch
                }
                getTemplates().collect { templates ->
                    _uiState.update { state ->
                        val selected = state.selectedTemplateId?.takeIf { id ->
                            templates.any { it.idPlantilla == id }
                        }
                        state.copy(
                            load = if (templates.isEmpty()) {
                                AnimalScheduleLoad.Empty(animal)
                            } else {
                                AnimalScheduleLoad.Ready(animal, templates)
                            },
                            selectedTemplateId = selected,
                            step = if (selected == null) AnimalScheduleStep.SelectTemplate else state.step,
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(load = AnimalScheduleLoad.Error("No se pudieron cargar los datos.")) }
            }
        }
    }

    fun selectTemplate(templateId: Long) {
        val state = _uiState.value
        val ready = state.load as? AnimalScheduleLoad.Ready ?: return
        if (state.isSaving || state.step != AnimalScheduleStep.SelectTemplate) return
        if (ready.templates.none { it.idPlantilla == templateId }) return
        _uiState.update { it.copy(selectedTemplateId = templateId) }
    }

    fun continueToConfigure() {
        if (!_uiState.value.canContinue) return
        _uiState.update { it.copy(step = AnimalScheduleStep.Configure) }
    }

    fun backToSelection(): Boolean {
        val state = _uiState.value
        if (state.isSaving) return true
        if (state.step != AnimalScheduleStep.Configure) return false
        _uiState.update { it.copy(step = AnimalScheduleStep.SelectTemplate) }
        return true
    }

    fun changeStart(date: LocalDate) = updateDraft {
        it.copy(fechaInicio = date, fieldErrors = it.fieldErrors - ScheduleField.FechaInicio)
    }

    fun changeEnd(date: LocalDate) = updateDraft {
        it.copy(fechaFin = date, fieldErrors = it.fieldErrors - ScheduleField.FechaFin)
    }

    fun changeTime(time: LocalTime) = updateDraft {
        it.copy(
            hora = LocalTime.of(time.hour, time.minute),
            fieldErrors = it.fieldErrors - ScheduleField.Hora,
        )
    }

    fun changeRecurrence(value: RecurrenciaNotificacion) = updateDraft {
        it.copy(recurrencia = value, fieldErrors = it.fieldErrors - ScheduleField.FechaFin)
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.step != AnimalScheduleStep.Configure) return
        val template = state.selectedTemplate ?: return
        val draft = state.draft
        val errors = linkedMapOf<ScheduleField, String>()
        if (draft.fechaInicio == null) errors[ScheduleField.FechaInicio] = "Selecciona la fecha de inicio."
        if (draft.fechaFin == null) errors[ScheduleField.FechaFin] = "Selecciona la fecha de fin."
        if (draft.hora == null) errors[ScheduleField.Hora] = "Selecciona la hora."
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(draft = draft.copy(fieldErrors = errors)) }
            return
        }
        val request = CreateAnimalNotificationScheduleRequest(
            templateId = template.idPlantilla,
            animalId = animalId,
            fechaInicio = draft.fechaInicio!!,
            fechaFin = draft.fechaFin!!,
            hora = LocalTime.of(draft.hora!!.hour, draft.hora.minute),
            recurrencia = draft.recurrencia,
        )
        _uiState.update { it.copy(isSaving = true, draft = it.draft.copy(fieldErrors = emptyMap())) }
        viewModelScope.launch {
            try {
                when (val result = createSchedule(request, nowMillis(), zoneId)) {
                    is CreateAnimalNotificationScheduleResult.Success ->
                        _events.emit(AnimalScheduleEvent.Completed)
                    is CreateAnimalNotificationScheduleResult.InvalidInput -> applyInvalid(result.error)
                    is CreateAnimalNotificationScheduleResult.SchedulingFailure ->
                        _events.emit(AnimalScheduleEvent.SchedulingFailed(result.scheduleId))
                    CreateAnimalNotificationScheduleResult.NoAuthenticatedUser ->
                        _events.emit(AnimalScheduleEvent.Message("No hay una sesión activa."))
                    CreateAnimalNotificationScheduleResult.TemplateNotFound ->
                        _events.emit(AnimalScheduleEvent.Message("No se encontró la plantilla."))
                    CreateAnimalNotificationScheduleResult.AnimalNotFound ->
                        _events.emit(AnimalScheduleEvent.Message("No se encontró el animal."))
                    CreateAnimalNotificationScheduleResult.OwnershipMismatch ->
                        _events.emit(AnimalScheduleEvent.Message("La plantilla o el animal no pertenecen a la sesión."))
                    is CreateAnimalNotificationScheduleResult.Failure ->
                        _events.emit(AnimalScheduleEvent.Message("No se pudo guardar la programación."))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _events.emit(AnimalScheduleEvent.Message("No se pudo guardar la programación."))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun applyInvalid(error: ScheduleValidationError) {
        val errors = when (error) {
            ScheduleValidationError.END_BEFORE_START -> mapOf(
                ScheduleField.FechaFin to "La fecha fin no puede ser anterior a la fecha inicio.",
            )
            ScheduleValidationError.NO_REQUIRES_SAME_END_DATE -> mapOf(
                ScheduleField.FechaFin to "Sin recurrencia, la fecha fin debe ser igual a la fecha inicio.",
            )
            ScheduleValidationError.INVALID_TIME_PRECISION -> mapOf(
                ScheduleField.Hora to "La hora debe indicarse en minutos.",
            )
            ScheduleValidationError.NO_FUTURE_OCCURRENCE -> mapOf(
                ScheduleField.FechaInicio to "La programación debe tener una ocurrencia futura.",
            )
            ScheduleValidationError.INVALID_TEMPLATE_ID,
            ScheduleValidationError.INVALID_ANIMAL_ID,
            ScheduleValidationError.INVALID_SCHEDULE_ID,
            -> emptyMap()
        }
        if (errors.isEmpty()) {
            _events.emit(AnimalScheduleEvent.Message("Revisa los datos de la programación."))
        } else {
            _uiState.update { it.copy(draft = it.draft.copy(fieldErrors = errors)) }
        }
    }

    private fun updateDraft(transform: (ScheduleDraft) -> ScheduleDraft) {
        val state = _uiState.value
        if (state.isSaving || state.step != AnimalScheduleStep.Configure) return
        _uiState.update { it.copy(draft = transform(it.draft)) }
    }
}
