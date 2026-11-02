package com.proyecto.ganapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Contrato físico futuro; no registrado en AppDatabase v10. */
@Entity(
    tableName = "plantilla_notificacion",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("idUsuario")],
)
data class PlantillaNotificacionEntity(
    @PrimaryKey(autoGenerate = true)
    val idPlantilla: Long = 0L,
    val titulo: String,
    val tipo: String,
    val idUsuario: Long,
    val legacyDescripcion: String? = null,
    val legacyEstado: String? = null,
    val legacyFechaCreacion: Long? = null,
    val legacyDosisPorDia: Int? = null,
    val legacyIntervaloHoras: Int? = null,
)
