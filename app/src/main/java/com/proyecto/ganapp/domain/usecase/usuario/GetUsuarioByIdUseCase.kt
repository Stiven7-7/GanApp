package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.repository.UsuarioRepository
import javax.inject.Inject

class GetUsuarioByIdUseCase @Inject constructor(
    private val repository: UsuarioRepository
) {
    suspend operator fun invoke(id: Long) = repository.getUserById(id)
}
