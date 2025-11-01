package com.proyecto.ganapp.data.local.dao

import androidx.room.*
import com.proyecto.ganapp.data.local.entity.NotificacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notificacion: NotificacionEntity): Long

    @Update
    suspend fun update(notificacion: NotificacionEntity)

    @Query("DELETE FROM notificacion WHERE idNotificacion = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM notificacion WHERE idNotificacion = :id")
    suspend fun getById(id: Long): NotificacionEntity?

    @Query("SELECT * FROM notificacion")
    fun getAll(): Flow<List<NotificacionEntity>>

    @Query("SELECT * FROM notificacion WHERE fechaFin >= :fechaActual")
    fun getActiveNotifications(fechaActual: String): Flow<List<NotificacionEntity>>
}
