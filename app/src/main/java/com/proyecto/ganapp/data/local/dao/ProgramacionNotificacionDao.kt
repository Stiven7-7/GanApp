package com.proyecto.ganapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.proyecto.ganapp.data.local.entity.ProgramacionNotificacionEntity

/** Puerto físico futuro, no expuesto por AppDatabase v10. */
@Dao
interface ProgramacionNotificacionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: ProgramacionNotificacionEntity): Long

    @Query("SELECT * FROM programacion_notificacion WHERE idProgramacion = :id")
    suspend fun getById(id: Long): ProgramacionNotificacionEntity?

    @Update
    suspend fun update(schedule: ProgramacionNotificacionEntity): Int

    @Query("DELETE FROM programacion_notificacion WHERE idProgramacion = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM programacion_notificacion WHERE idAnimal = :animalId")
    suspend fun getByAnimal(animalId: Long): List<ProgramacionNotificacionEntity>

    @Query("SELECT * FROM programacion_notificacion WHERE idPlantilla = :templateId")
    suspend fun getByTemplate(templateId: Long): List<ProgramacionNotificacionEntity>

    @Query("SELECT COUNT(*) FROM programacion_notificacion WHERE idPlantilla = :templateId")
    suspend fun countByTemplate(templateId: Long): Long
}
