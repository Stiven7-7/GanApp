package com.proyecto.ganapp.domain.model

data class Animal(
    val idAnimal: Long = 0L,
    val especie: EspecieAnimal,
    val nombre: String,
    val raza: String,
    val edad: Int?,
    val peso: Float?,
    val color: String?,
    val fotoUri: String?,
    val idUsuario: Long,
    val esFavorito: Boolean = false,
)
