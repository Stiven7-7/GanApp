package com.proyecto.ganapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.proyecto.ganapp.util.DateUtils

@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val idUsuario: Long = 0,
    val nombre: String,
    val apellido: String,
    val correo: String,
    @ColumnInfo(name = "contrasena") val contrasena: String,
    val fechaRegistro: String = DateUtils.currentDate(), // 👈 aquí guardamos el texto legible
    val activo: Boolean = true
)
