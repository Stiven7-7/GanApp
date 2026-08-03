package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.model.AuthenticatedUser
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import java.util.Locale

class LoginUseCase(
    private val repository: UsuarioRepository
) {
    suspend operator fun invoke(
        correo: String,
        contrasena: String,
    ): LoginResult {
        val normalizedCorreo = correo.trim().lowercase(Locale.ROOT)

        val errors = linkedSetOf<LoginValidationError>()

        when {
            normalizedCorreo.isEmpty() -> {
                errors += LoginValidationError.EMPTY_EMAIL
            }
            normalizedCorreo.any { it.isWhitespace() } -> {
                errors += LoginValidationError.EMAIL_CONTAINS_WHITESPACE
            }
            !hasValidEmailFormat(normalizedCorreo) -> {
                errors += LoginValidationError.INVALID_EMAIL_FORMAT
            }
        }

        when {
            contrasena.isEmpty() -> {
                errors += LoginValidationError.EMPTY_PASSWORD
            }
            contrasena.length > MAX_PASSWORD_LENGTH -> {
                errors += LoginValidationError.PASSWORD_TOO_LONG
            }
        }

        if (errors.isNotEmpty()) {
            return LoginResult.ValidationError(errors.toSet())
        }

        return try {
            val usuario = repository.login(normalizedCorreo, contrasena)
            if (usuario == null) {
                LoginResult.InvalidCredentials
            } else if (usuario.idUsuario <= 0) {
                LoginResult.UnexpectedError
            } else {
                LoginResult.Success(
                    AuthenticatedUser(
                        idUsuario = usuario.idUsuario,
                        nombre = usuario.nombre,
                        apellido = usuario.apellido,
                        correo = usuario.correo,
                    )
                )
            }
        } catch (_: Exception) {
            LoginResult.UnexpectedError
        }
    }

    /**
     * Validación de correo en JVM puro: una sola '@', local y dominio no vacíos,
     * dominio con al menos un punto no terminal/inicial. Espacios se validan aparte.
     */
    private fun hasValidEmailFormat(email: String): Boolean {
        val atIndex = email.indexOf('@')
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false
        }
        val local = email.substring(0, atIndex)
        val domain = email.substring(atIndex + 1)
        if (local.isEmpty() || domain.isEmpty()) {
            return false
        }
        if (!domain.contains('.')) {
            return false
        }
        if (domain.startsWith('.') || domain.endsWith('.')) {
            return false
        }
        return true
    }

    private companion object {
        const val MAX_PASSWORD_LENGTH = 64
    }
}
