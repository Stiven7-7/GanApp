package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.UsuarioRepository

/**
 * Fake manual de [UsuarioRepository] para pruebas unitarias (sin frameworks de mock).
 */
class FakeUsuarioRepository : UsuarioRepository {
    var registerReturnId: Long = 1L
    var registerException: Exception? = null
    var registerCallCount: Int = 0
        private set
    var lastRegisteredUsuario: Usuario? = null
        private set

    override suspend fun register(usuario: Usuario): Long {
        registerCallCount++
        lastRegisteredUsuario = usuario
        registerException?.let { throw it }
        return registerReturnId
    }

    override suspend fun login(correo: String, contrasena: String): Usuario? =
        error("Unexpected call to login")

    override suspend fun getUserById(id: Long): Usuario? =
        error("Unexpected call to getUserById")

    override suspend fun getUsuarioByCorreo(correo: String): Usuario? =
        error("Unexpected call to getUsuarioByCorreo")
}
