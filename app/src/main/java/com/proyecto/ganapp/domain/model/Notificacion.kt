package com.proyecto.ganapp.domain.model

data class Notificacion(
    val idNotificacion: Long = 0,
    val nombre: String,
    val tipo: String,
    val fechaInicio: Long,
    val fechaFin: Long,
    val seRepite: String,
    val idUsuario: Long
)
