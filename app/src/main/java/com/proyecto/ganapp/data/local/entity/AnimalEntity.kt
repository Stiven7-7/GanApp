package com.proyecto.ganapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "animal",
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
data class AnimalEntity(
    @PrimaryKey(autoGenerate = true) val idAnimal: Long = 0,
    val nombre: String,
    val edad: Int,
    val peso: Float,
    val color: String,
    val raza: String? = null,
    val idUsuario: Long,
    val fotoUri: String? = null
    )
