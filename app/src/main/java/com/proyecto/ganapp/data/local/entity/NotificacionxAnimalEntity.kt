package com.proyecto.ganapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "notificacion_animal",
    primaryKeys = ["idAnimal", "idNotificacion"],
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["idAnimal"],
            childColumns = ["idAnimal"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NotificacionEntity::class,
            parentColumns = ["idNotificacion"],
            childColumns = ["idNotificacion"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idAnimal"), Index("idNotificacion")]
)
data class NotificacionxAnimalEntity(
    val idAnimal: Long,
    val idNotificacion: Long
)
