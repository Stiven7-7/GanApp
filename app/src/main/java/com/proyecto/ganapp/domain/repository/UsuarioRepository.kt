package com.proyecto.ganapp.domain.repository

import com.proyecto.ganapp.domain.model.Usuario

interface UsuarioRepository {
    suspend fun register(usuario: Usuario): Long
    suspend fun login(correo: String, contrasena: String): Usuario?
    suspend fun getUserById(id: Long): Usuario?
    suspend fun getUsuarioByCorreo(correo: String): Usuario?

}
