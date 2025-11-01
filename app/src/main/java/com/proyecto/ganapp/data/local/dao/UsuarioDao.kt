package com.proyecto.ganapp.data.local.dao

import androidx.room.*
import com.proyecto.ganapp.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: UsuarioEntity): Long

    @Update
    suspend fun update(usuario: UsuarioEntity)

    @Delete
    suspend fun delete(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuario WHERE idUsuario = :id LIMIT 1")
    suspend fun getById(id: Long): UsuarioEntity?

    @Query("SELECT * FROM usuario")
    fun getAll(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuario WHERE correo = :correo AND contrasena = :contrasena LIMIT 1")
    suspend fun login(correo: String, contrasena: String): UsuarioEntity?

    @Query("SELECT * FROM usuario WHERE correo = :correo LIMIT 1")
    suspend fun getByCorreo(correo: String): UsuarioEntity?
}
