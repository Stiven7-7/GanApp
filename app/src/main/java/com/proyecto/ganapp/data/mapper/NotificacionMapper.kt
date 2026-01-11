package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.NotificacionEntity
import com.proyecto.ganapp.domain.model.Notificacion

fun NotificacionEntity.toDomain(): Notificacion =
    Notificacion(
        idNotificacion = idNotificacion,
        nombre = nombre,
        tipo = tipo,
        fechaInicio = fechaInicio,
        fechaFin = fechaFin ?: fechaInicio,
        seRepite = seRepite,
        hora = hora,
        dosisPorDia = dosisPorDia,
        intervaloHoras = intervaloHoras,
        idUsuario = idUsuario
    )

fun Notificacion.toEntity(): NotificacionEntity =
    NotificacionEntity(
        idNotificacion = idNotificacion,
        nombre = nombre,
        tipo = tipo,
        descripcion = null, // si luego quieres usarlo, lo agregas en el modelo
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
        seRepite = seRepite,
        hora = hora,
        dosisPorDia = dosisPorDia,
        intervaloHoras = intervaloHoras,
        idUsuario = idUsuario
    )
