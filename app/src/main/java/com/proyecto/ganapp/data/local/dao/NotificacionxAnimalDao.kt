package com.proyecto.ganapp.data.local.dao

import androidx.room.*
import com.proyecto.ganapp.data.local.entity.NotificacionxAnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionxAnimalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relacion: NotificacionxAnimalEntity): Long

    @Delete
    suspend fun delete(relacion: NotificacionxAnimalEntity)

    @Query("""
        SELECT * FROM notificacion_animal 
        WHERE idAnimal = :idAnimal
    """)
    fun getByAnimal(idAnimal: Long): Flow<List<NotificacionxAnimalEntity>>

    @Query("""
        SELECT * FROM notificacion_animal 
        WHERE idNotificacion = :idNotificacion
    """)
    fun getByNotificacion(idNotificacion: Long): Flow<List<NotificacionxAnimalEntity>>

    @Query("DELETE FROM notificacion_animal WHERE idAnimal = :idAnimal")
    suspend fun deleteByAnimal(idAnimal: Long)
}
