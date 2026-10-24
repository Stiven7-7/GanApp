package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion

object CreateNotificationMapper {
    fun toNotificacion(request: CreateNotificationRequest): Notificacion {
        return Notificacion(
            idNotificacion = 0L,
            nombre = request.nombre.trim(),
            tipo = request.tipo.toPersistentString(),
            fechaInicio = request.fechaInicio,
            fechaFin = request.fechaFin,
            seRepite = request.recurrencia.toPersistentString(),
            hora = request.hora,
            dosisPorDia = null,
            intervaloHoras = null,
            idUsuario = request.idUsuario,
        )
    }
}
