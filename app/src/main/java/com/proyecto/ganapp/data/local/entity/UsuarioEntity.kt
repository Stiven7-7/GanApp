package com.proyecto.ganapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val idUsuario: Long = 0,
    val nombre: String,
    val apellido: String,
    val correo: String,
    @ColumnInfo(name = "contrasena") val contrasena: String,
    val fechaRegistro: Long = System.currentTimeMillis(),
    val activo: Boolean = true
)
