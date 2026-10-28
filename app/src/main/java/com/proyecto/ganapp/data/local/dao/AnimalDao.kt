package com.proyecto.ganapp.data.local.dao

import androidx.room.*
import com.proyecto.ganapp.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(animal: AnimalEntity): Long

    @Update
    suspend fun update(animal: AnimalEntity): Int

    @Query("DELETE FROM animal WHERE idAnimal = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM animal WHERE idAnimal = :id")
    suspend fun getById(id: Long): AnimalEntity?

    @Query("SELECT * FROM animal WHERE idUsuario = :userId")
    fun getByUser(userId: Long): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animal WHERE idUsuario = :userId")
    fun getAnimalsByUser(userId: Long): Flow<List<AnimalEntity>>

    @Query(
        """
        SELECT *
        FROM animal
        WHERE idUsuario = :userId
          AND esFavorito = 1
        """
    )
    fun getFavoriteAnimalsByUser(userId: Long): Flow<List<AnimalEntity>>
    @Query("SELECT * FROM animal")
    fun getAll(): Flow<List<AnimalEntity>>
}
