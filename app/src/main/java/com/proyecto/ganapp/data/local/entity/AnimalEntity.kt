package com.proyecto.ganapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.proyecto.ganapp.domain.model.EspecieAnimal

@Entity(
    tableName = "animal",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("idUsuario")],
)
data class AnimalEntity(
    @PrimaryKey(autoGenerate = true) val idAnimal: Long = 0,
    val especie: EspecieAnimal,
    val nombre: String,
    val raza: String,
    val edad: Int?,
    val peso: Float?,
    val color: String?,
    val idUsuario: Long,
    val fotoUri: String?,
    @ColumnInfo(defaultValue = "0")
    val esFavorito: Boolean = false,
)
