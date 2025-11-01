package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.UsuarioRepository

class RegisterUserUseCase(
    private val repository: UsuarioRepository
) {
    suspend operator fun invoke(usuario: Usuario): Long {
        return repository.register(usuario)
    }
}
