package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.UsuarioRepository

class LoginUseCase(
    private val repository: UsuarioRepository
) {
    suspend operator fun invoke(correo: String, contrasena: String): Usuario? {
        return repository.login(correo, contrasena)
    }
}
