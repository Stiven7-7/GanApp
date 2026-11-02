package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.PlantillaNotificacionEntity
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion

fun PlantillaNotificacionEntity.toDomain(): PlantillaNotificacion =
    PlantillaNotificacion(
        idPlantilla = idPlantilla,
        titulo = titulo,
        tipo = requireNotNull(TipoNotificacion.fromPersistentString(tipo)) {
            "Unknown notification template type: $tipo"
        },
        idUsuario = idUsuario,
    )

fun PlantillaNotificacion.toEntity(): PlantillaNotificacionEntity =
    PlantillaNotificacionEntity(
        idPlantilla = idPlantilla,
        titulo = titulo,
        tipo = tipo.toPersistentString(),
        idUsuario = idUsuario,
    )
