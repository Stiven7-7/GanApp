package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.model.AuthenticatedUser
import com.proyecto.ganapp.domain.model.Usuario
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private lateinit var repository: FakeLoginUsuarioRepository
    private lateinit var useCase: LoginUseCase

    @Before
    fun setUp() {
        repository = FakeLoginUsuarioRepository()
        useCase = LoginUseCase(repository)
    }

    @Test
    fun loginValido_retornaSuccess() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario(id = 10L)

        val result = useCase(
            correo = "ana@example.com",
            contrasena = "password1",
        )

        assertEquals(
            LoginResult.Success(
                AuthenticatedUser(
                    idUsuario = 10L,
                    nombre = "Ana",
                    apellido = "Pérez",
                    correo = "ana@example.com",
                )
            ),
            result,
        )
    }

    @Test
    fun correo_seNormalizaConTrim() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario()

        useCase(
            correo = "  ana@example.com  ",
            contrasena = "password1",
        )

        assertEquals("ana@example.com", repository.lastLoginCorreo)
    }

    @Test
    fun correo_seNormalizaAMinusculas() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario()

        useCase(
            correo = "Ana.Perez@Example.COM",
            contrasena = "password1",
        )

        assertEquals("ana.perez@example.com", repository.lastLoginCorreo)
    }

    @Test
    fun contrasena_noSeTransforma() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario()
        val password = "  Pass Word  "

        useCase(
            correo = "ana@example.com",
            contrasena = password,
        )

        assertEquals(password, repository.lastLoginContrasena)
    }

    @Test
    fun correoVacio_produceError() = runBlocking {
        val result = useCase(
            correo = "   ",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.EMPTY_EMAIL)
        assertEquals(0, repository.loginCallCount)
    }

    @Test
    fun correoConEspaciosInternos_produceError() = runBlocking {
        val result = useCase(
            correo = "ana perez@example.com",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.EMAIL_CONTAINS_WHITESPACE)
    }

    @Test
    fun correoSinArroba_produceError() = runBlocking {
        val result = useCase(
            correo = "ana.example.com",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun correoConMasDeUnaArroba_produceError() = runBlocking {
        val result = useCase(
            correo = "ana@ex@ample.com",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun parteLocalVacia_produceError() = runBlocking {
        val result = useCase(
            correo = "@example.com",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun dominioVacio_produceError() = runBlocking {
        val result = useCase(
            correo = "ana@",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun dominioSinPunto_produceError() = runBlocking {
        val result = useCase(
            correo = "ana@example",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun dominioIniciaConPunto_produceError() = runBlocking {
        val result = useCase(
            correo = "ana@.example.com",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun dominioTerminaConPunto_produceError() = runBlocking {
        val result = useCase(
            correo = "ana@example.com.",
            contrasena = "password1",
        )

        assertValidationContains(result, LoginValidationError.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun contrasenaVacia_produceError() = runBlocking {
        val result = useCase(
            correo = "ana@example.com",
            contrasena = "",
        )

        assertValidationContains(result, LoginValidationError.EMPTY_PASSWORD)
    }

    @Test
    fun contrasenaDe64Caracteres_esValida() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario()
        val password = "a".repeat(64)

        val result = useCase(
            correo = "ana@example.com",
            contrasena = password,
        )

        assertTrue(result is LoginResult.Success)
        assertEquals(password, repository.lastLoginContrasena)
    }

    @Test
    fun contrasenaDe65Caracteres_esInvalida() = runBlocking {
        val result = useCase(
            correo = "ana@example.com",
            contrasena = "a".repeat(65),
        )

        assertValidationContains(result, LoginValidationError.PASSWORD_TOO_LONG)
        assertEquals(0, repository.loginCallCount)
    }

    @Test
    fun multiplesErrores_retornanSimultaneos() = runBlocking {
        val result = useCase(
            correo = "",
            contrasena = "",
        )

        assertTrue(result is LoginResult.ValidationError)
        val errors = (result as LoginResult.ValidationError).errors
        assertTrue(errors.contains(LoginValidationError.EMPTY_EMAIL))
        assertTrue(errors.contains(LoginValidationError.EMPTY_PASSWORD))
        assertEquals(2, errors.size)
    }

    @Test
    fun repositorioNoSeInvoca_siHayErrores() = runBlocking {
        useCase(
            correo = "no-es-correo",
            contrasena = "password1",
        )

        assertEquals(0, repository.loginCallCount)
        assertNull(repository.lastLoginCorreo)
        assertNull(repository.lastLoginContrasena)
    }

    @Test
    fun repositorioRetornaNull_produceInvalidCredentials() = runBlocking {
        repository.loginReturnUsuario = null

        val result = useCase(
            correo = "ana@example.com",
            contrasena = "password1",
        )

        assertEquals(LoginResult.InvalidCredentials, result)
    }

    @Test
    fun usuarioValido_produceSuccess() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario(id = 99L, correo = "user@example.com")

        val result = useCase(
            correo = "user@example.com",
            contrasena = "password1",
        )

        assertTrue(result is LoginResult.Success)
        assertEquals(99L, (result as LoginResult.Success).authenticatedUser.idUsuario)
    }

    @Test
    fun authenticatedUser_noContieneContrasena() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario(password = "secreto-hash")

        val result = useCase(
            correo = "ana@example.com",
            contrasena = "password1",
        )

        assertTrue(result is LoginResult.Success)
        val authenticatedUser = (result as LoginResult.Success).authenticatedUser
        val serialized = authenticatedUser.toString()
        assertFalse(serialized.contains("secreto-hash"))
        assertFalse(serialized.contains("contrasena"))
        val propertyNames = AuthenticatedUser::class.java.declaredFields
            .map { it.name }
            .filterNot { it == "Companion" || it.startsWith("$") || it.contains("stable", ignoreCase = true) }
            .toSet()
        assertTrue(propertyNames.containsAll(setOf("idUsuario", "nombre", "apellido", "correo")))
        assertFalse(propertyNames.any { it.contains("contrasena", ignoreCase = true) })
        assertFalse(propertyNames.any { it.contains("password", ignoreCase = true) })
        assertFalse(propertyNames.any { it.contains("hash", ignoreCase = true) })
    }

    @Test
    fun idCero_produceUnexpectedError() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario(id = 0L)

        val result = useCase(
            correo = "ana@example.com",
            contrasena = "password1",
        )

        assertEquals(LoginResult.UnexpectedError, result)
    }

    @Test
    fun idNegativo_produceUnexpectedError() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario(id = -5L)

        val result = useCase(
            correo = "ana@example.com",
            contrasena = "password1",
        )

        assertEquals(LoginResult.UnexpectedError, result)
    }

    @Test
    fun excepcionDelRepositorio_produceUnexpectedError() = runBlocking {
        repository.loginException = RuntimeException("fallo de persistencia")

        val result = useCase(
            correo = "ana@example.com",
            contrasena = "password1",
        )

        assertEquals(LoginResult.UnexpectedError, result)
    }

    @Test
    fun repositorioSeInvocaUnaSolaVez() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario()

        useCase(
            correo = "ana@example.com",
            contrasena = "password1",
        )

        assertEquals(1, repository.loginCallCount)
    }

    @Test
    fun repositorioRecibeCorreoNormalizado() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario()

        useCase(
            correo = "  Ana@Example.COM  ",
            contrasena = "password1",
        )

        assertEquals("ana@example.com", repository.lastLoginCorreo)
    }

    @Test
    fun repositorioRecibeContrasenaOriginal() = runBlocking {
        repository.loginReturnUsuario = sampleUsuario()
        val password = "MiClave Con Espacios!"

        useCase(
            correo = "ana@example.com",
            contrasena = password,
        )

        assertEquals(password, repository.lastLoginContrasena)
    }

    private fun sampleUsuario(
        id: Long = 1L,
        correo: String = "ana@example.com",
        password: String = "hash-placeholder",
    ): Usuario = Usuario(
        idUsuario = id,
        nombre = "Ana",
        apellido = "Pérez",
        correo = correo,
        contrasena = password,
    )

    private fun assertValidationContains(
        result: LoginResult,
        error: LoginValidationError,
    ) {
        assertTrue(result is LoginResult.ValidationError)
        assertTrue((result as LoginResult.ValidationError).errors.contains(error))
    }
}
