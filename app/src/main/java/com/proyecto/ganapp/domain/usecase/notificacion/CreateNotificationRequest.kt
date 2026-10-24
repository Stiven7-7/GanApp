package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion

data class CreateNotificationRequest(
    val nombre: String,
    val tipo: TipoNotificacion,
    val fechaInicio: Long,
    val fechaFin: Long,
    val hora: String,
    val recurrencia: RecurrenciaNotificacion,
    val idUsuario: Long,
)
