package com.proyecto.ganapp.domain.model

data class PlantillaNotificacion(
    val idPlantilla: Long = 0L,
    val titulo: String,
    val tipo: TipoNotificacion,
    val idUsuario: Long,
)
