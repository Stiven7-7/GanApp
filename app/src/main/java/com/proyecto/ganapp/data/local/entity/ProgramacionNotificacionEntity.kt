package com.proyecto.ganapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Contrato físico futuro; idAnimal null preserva exclusivamente compatibilidad legacy. */
@Entity(
    tableName = "programacion_notificacion",
    foreignKeys = [
        ForeignKey(
            entity = PlantillaNotificacionEntity::class,
            parentColumns = ["idPlantilla"],
            childColumns = ["idPlantilla"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["idAnimal"],
            childColumns = ["idAnimal"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("idPlantilla"), Index("idAnimal")],
)
data class ProgramacionNotificacionEntity(
    @PrimaryKey(autoGenerate = true)
    val idProgramacion: Long = 0L,
    val idPlantilla: Long,
    val idAnimal: Long?,
    val fechaInicio: Long,
    val fechaFin: Long,
    val hora: String,
    val recurrencia: String,
)
