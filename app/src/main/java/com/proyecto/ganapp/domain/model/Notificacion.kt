package com.proyecto.ganapp.domain.model

data class Notificacion(
    val idNotificacion: Long = 0,
    val nombre: String,
    val tipo: String,
    val fechaInicio: Long,
    val fechaFin: Long,
    val seRepite: String,
    val hora: String,
    val dosisPorDia: Int? = null,
    val intervaloHoras: Int? = null,
    val idUsuario: Long
)
