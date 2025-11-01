package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.UsuarioEntity
import com.proyecto.ganapp.domain.model.Usuario

fun UsuarioEntity.toDomain(): Usuario = Usuario(
    idUsuario = idUsuario,
    nombre = nombre,
    apellido = apellido,
    correo = correo,
    contrasena = contrasena
)

fun Usuario.toEntity(): UsuarioEntity = UsuarioEntity(
    idUsuario = idUsuario,
    nombre = nombre,
    apellido = apellido,
    correo = correo,
    contrasena = contrasena
)
