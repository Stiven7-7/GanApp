package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.UsuarioRepository

/**
 * Fake manual de [UsuarioRepository] orientado a pruebas de [LoginUseCase].
 * Fallan de inmediato las operaciones ajenas al login.
 */
class FakeLoginUsuarioRepository : UsuarioRepository {
    var loginReturnUsuario: Usuario? = null
    var loginException: Exception? = null
    var loginCallCount: Int = 0
        private set
    var lastLoginCorreo: String? = null
        private set
    var lastLoginContrasena: String? = null
        private set

    override suspend fun login(correo: String, contrasena: String): Usuario? {
        loginCallCount++
        lastLoginCorreo = correo
        lastLoginContrasena = contrasena
        loginException?.let { throw it }
        return loginReturnUsuario
    }

    override suspend fun register(usuario: Usuario): Long =
        error("Unexpected call to register")

    override suspend fun getUserById(id: Long): Usuario? =
        error("Unexpected call to getUserById")

    override suspend fun getUsuarioByCorreo(correo: String): Usuario? =
        error("Unexpected call to getUsuarioByCorreo")
}
