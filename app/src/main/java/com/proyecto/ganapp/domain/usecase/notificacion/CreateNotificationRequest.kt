package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion

data class CreateNotificationRequest(
    val nombre: String,
    val tipo: TipoNotificacion,
    /** Día calendario como `LocalDate.toEpochDay()` desde el schema v9. */
    val fechaInicio: Long,
    /** Día calendario como `LocalDate.toEpochDay()` desde el schema v9. */
    val fechaFin: Long,
    val hora: String,
    val recurrencia: RecurrenciaNotificacion,
    val idUsuario: Long,
)
