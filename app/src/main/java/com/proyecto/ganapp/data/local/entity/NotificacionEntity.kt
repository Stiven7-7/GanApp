package com.proyecto.ganapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notificacion",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idUsuario")]
)
data class NotificacionEntity(
    @PrimaryKey(autoGenerate = true)
    val idNotificacion: Long = 0,
    val nombre: String,
    val tipo: String,
    val descripcion: String? = null,
    val fechaInicio: Long,
    val fechaFin: Long?,
    val seRepite: String, // “NO”, “DIARIA”, “MENSUAL”, “ANUAL”
    val hora: String? = null,
    val estado: String = "PENDIENTE",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val idUsuario: Long
)
