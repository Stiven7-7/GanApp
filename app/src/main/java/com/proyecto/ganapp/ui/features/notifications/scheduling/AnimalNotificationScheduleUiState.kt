package com.proyecto.ganapp.ui.features.notifications.scheduling

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.LocalDate
import java.time.LocalTime

sealed interface AnimalScheduleLoad {
    data object Loading : AnimalScheduleLoad
    data class Error(val message: String) : AnimalScheduleLoad
    data class Empty(val animal: Animal) : AnimalScheduleLoad
    data class Ready(val animal: Animal, val templates: List<PlantillaNotificacion>) : AnimalScheduleLoad
}

enum class AnimalScheduleStep {
    SelectTemplate,
    Configure,
}

enum class ScheduleField {
    FechaInicio,
    FechaFin,
    Hora,
}

data class ScheduleDraft(
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null,
    val hora: LocalTime? = null,
    val recurrencia: RecurrenciaNotificacion = RecurrenciaNotificacion.NO,
    val fieldErrors: Map<ScheduleField, String> = emptyMap(),
)

data class AnimalNotificationScheduleUiState(
    val load: AnimalScheduleLoad = AnimalScheduleLoad.Loading,
    val selectedTemplateId: Long? = null,
    val step: AnimalScheduleStep = AnimalScheduleStep.SelectTemplate,
    val draft: ScheduleDraft = ScheduleDraft(),
    val isSaving: Boolean = false,
) {
    val animal: Animal?
        get() = when (val current = load) {
            is AnimalScheduleLoad.Empty -> current.animal
            is AnimalScheduleLoad.Ready -> current.animal
            else -> null
        }

    val selectedTemplate: PlantillaNotificacion?
        get() {
            val ready = load as? AnimalScheduleLoad.Ready ?: return null
            return ready.templates.firstOrNull { it.idPlantilla == selectedTemplateId }
        }

    val canContinue: Boolean
        get() = !isSaving && step == AnimalScheduleStep.SelectTemplate && selectedTemplate != null
}

sealed interface AnimalScheduleEvent {
    data class Message(val text: String) : AnimalScheduleEvent
    data class SchedulingFailed(val scheduleId: Long) : AnimalScheduleEvent
    data object Completed : AnimalScheduleEvent
}
