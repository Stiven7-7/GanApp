package com.proyecto.ganapp.domain.model

data class Usuario(
    val idUsuario: Long = 0,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val contrasena: String
)
