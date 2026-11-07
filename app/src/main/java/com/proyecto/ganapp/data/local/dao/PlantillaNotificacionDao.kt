package com.proyecto.ganapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.proyecto.ganapp.data.local.entity.PlantillaNotificacionEntity
import kotlinx.coroutines.flow.Flow

/** Puerto físico de Plantillas expuesto por AppDatabase v11. */
@Dao
interface PlantillaNotificacionDao {
    // ABORT evita que un conflicto de PK elimine implícitamente una plantilla mediante REPLACE.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: PlantillaNotificacionEntity): Long

    @Query("SELECT * FROM plantilla_notificacion WHERE idPlantilla = :id")
    suspend fun getById(id: Long): PlantillaNotificacionEntity?

    @Query("SELECT * FROM plantilla_notificacion WHERE idUsuario = :userId")
    fun getByUser(userId: Long): Flow<List<PlantillaNotificacionEntity>>

    @Query("UPDATE plantilla_notificacion SET titulo = :titulo, tipo = :tipo WHERE idPlantilla = :idPlantilla")
    suspend fun updateContent(idPlantilla: Long, titulo: String, tipo: String): Int

    @Query("DELETE FROM plantilla_notificacion WHERE idPlantilla = :id")
    suspend fun deleteById(id: Long)
}
