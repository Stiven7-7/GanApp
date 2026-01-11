package com.proyecto.ganapp.data.local.dao

import androidx.room.*
import com.proyecto.ganapp.data.local.entity.NotificacionxAnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionxAnimalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: NotificacionxAnimalEntity)

    @Query("DELETE FROM notificacion_animal WHERE idAnimal = :idAnimal AND idNotificacion = :idNotificacion")
    suspend fun delete(idAnimal: Long, idNotificacion: Long)

    @Query("SELECT * FROM notificacion_animal WHERE idAnimal = :idAnimal")
    suspend fun getNotificacionesByAnimal(idAnimal: Long): List<NotificacionxAnimalEntity>

    @Query("SELECT * FROM notificacion_animal WHERE idNotificacion = :idNotificacion")
    suspend fun getAnimalesByNotificacion(idNotificacion: Long): List<NotificacionxAnimalEntity>

}

