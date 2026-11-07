package com.proyecto.ganapp.ui.features.notifications.animalschedules

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle

const val AnimalSchedulesEmptyMessage = "Aún no hay programaciones."
const val AnimalSchedulesErrorMessage = "No fue posible cargar las programaciones."

sealed interface AnimalSchedulesContent {
    data object Loading : AnimalSchedulesContent
    data class Empty(val animal: Animal) : AnimalSchedulesContent
    data class Error(val message: String) : AnimalSchedulesContent
    data class Success(
        val animal: Animal,
        val schedules: List<ProgramacionNotificacionDetalle>,
    ) : AnimalSchedulesContent
}
