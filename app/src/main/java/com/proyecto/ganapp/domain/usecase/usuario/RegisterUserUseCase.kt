package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import java.util.Locale

class RegisterUserUseCase(
    private val repository: UsuarioRepository
) {
    suspend operator fun invoke(
        nombre: String,
        apellido: String,
        correo: String,
        contrasena: String,
        confirmacionContrasena: String,
    ): RegisterUserResult {
        val normalizedNombre = nombre.trim()
        val normalizedApellido = apellido.trim()
        val normalizedCorreo = correo.trim().lowercase(Locale.ROOT)

        val errors = linkedSetOf<RegisterValidationError>()

        if (normalizedNombre.isEmpty()) {
            errors += RegisterValidationError.EMPTY_NAME
        }
        if (normalizedApellido.isEmpty()) {
            errors += RegisterValidationError.EMPTY_LAST_NAME
        }

        when {
            normalizedCorreo.isEmpty() -> {
                errors += RegisterValidationError.EMPTY_EMAIL
            }
            normalizedCorreo.any { it.isWhitespace() } -> {
                errors += RegisterValidationError.EMAIL_CONTAINS_WHITESPACE
            }
            !hasValidEmailFormat(normalizedCorreo) -> {
                errors += RegisterValidationError.INVALID_EMAIL_FORMAT
            }
        }

        when {
            contrasena.isEmpty() -> {
                errors += RegisterValidationError.EMPTY_PASSWORD
            }
            contrasena.length < MIN_PASSWORD_LENGTH -> {
                errors += RegisterValidationError.PASSWORD_TOO_SHORT
            }
            contrasena.length > MAX_PASSWORD_LENGTH -> {
                errors += RegisterValidationError.PASSWORD_TOO_LONG
            }
        }

        when {
            confirmacionContrasena.isEmpty() -> {
                errors += RegisterValidationError.EMPTY_CONFIRMATION
            }
            confirmacionContrasena != contrasena -> {
                errors += RegisterValidationError.PASSWORD_MISMATCH
            }
        }

        if (errors.isNotEmpty()) {
            return RegisterUserResult.ValidationError(errors.toSet())
        }

        return try {
            val usuario = Usuario(
                idUsuario = 0,
                nombre = normalizedNombre,
                apellido = normalizedApellido,
                correo = normalizedCorreo,
                contrasena = contrasena,
            )
            val userId = repository.register(usuario)
            if (userId > 0) {
                RegisterUserResult.Success(userId)
            } else {
                RegisterUserResult.UnexpectedError
            }
        } catch (_: Exception) {
            RegisterUserResult.UnexpectedError
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
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_PASSWORD_LENGTH = 64
    }
}
