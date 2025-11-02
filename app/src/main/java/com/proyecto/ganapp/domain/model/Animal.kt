package com.proyecto.ganapp.domain.model

data class Animal(
    val idAnimal: Long = 0,
    val nombre: String,
    val edad: Int,
    val peso: Float,
    val color: String,
    val idUsuario: Long,
    val fotoUri: String?
)
