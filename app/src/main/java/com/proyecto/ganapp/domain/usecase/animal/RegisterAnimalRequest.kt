package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.model.EspecieAnimal

data class RegisterAnimalRequest(
    val especie: EspecieAnimal?,
    val nombre: String,
    val edad: Int?,
    val peso: Float?,
    val color: String?,
    val fotoUri: String?,
    val idUsuario: Long,
)
