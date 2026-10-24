package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.AnimalEntity
import com.proyecto.ganapp.domain.model.Animal

fun AnimalEntity.toDomain(): Animal = Animal(
    idAnimal = idAnimal,
    especie = especie,
    nombre = nombre,
    raza = raza,
    edad = edad,
    peso = peso,
    color = color,
    fotoUri = fotoUri,
    idUsuario = idUsuario,
)

fun Animal.toEntity(): AnimalEntity = AnimalEntity(
    idAnimal = idAnimal,
    especie = especie,
    nombre = nombre,
    raza = raza,
    edad = edad,
    peso = peso,
    color = color,
    fotoUri = fotoUri,
    idUsuario = idUsuario,
)
