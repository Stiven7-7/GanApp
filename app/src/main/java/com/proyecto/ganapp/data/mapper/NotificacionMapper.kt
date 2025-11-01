package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.NotificacionEntity
import com.proyecto.ganapp.domain.model.Notificacion

fun NotificacionEntity.toDomain(): Notificacion = Notificacion(
    idNotificacion = idNotificacion,
    nombre = nombre,
    tipo = tipo,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin ?: 0L,
    seRepite = seRepite,
    idUsuario = idUsuario
)

fun Notificacion.toEntity(): NotificacionEntity = NotificacionEntity(
    idNotificacion = idNotificacion,
    nombre = nombre,
    tipo = tipo,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    seRepite = seRepite,
    idUsuario = idUsuario
)
