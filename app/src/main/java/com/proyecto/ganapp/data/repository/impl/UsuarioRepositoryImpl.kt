package com.proyecto.ganapp.data.repository.impl

import com.proyecto.ganapp.data.local.dao.UsuarioDao
import com.proyecto.ganapp.data.mapper.toDomain
import com.proyecto.ganapp.data.mapper.toEntity
import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val dao: UsuarioDao
) : UsuarioRepository {

    override suspend fun register(usuario: Usuario): Long {
        return dao.insert(usuario.toEntity())
    }

    override suspend fun login(correo: String, contrasena: String): Usuario? {
        return dao.login(correo, contrasena)?.toDomain()
    }

    override suspend fun getUserById(id: Long): Usuario? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getUsuarioByCorreo(correo: String): Usuario? {
        return dao.getByCorreo(correo)?.toDomain()
    }
}
