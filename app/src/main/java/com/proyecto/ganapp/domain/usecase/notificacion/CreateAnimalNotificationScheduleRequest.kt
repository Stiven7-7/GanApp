package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.LocalDate
import java.time.LocalTime

data class CreateAnimalNotificationScheduleRequest(
    val templateId: Long,
    val animalId: Long,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
    val hora: LocalTime,
    val recurrencia: RecurrenciaNotificacion,
)
