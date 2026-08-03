package com.proyecto.ganapp.domain.usecase.usuario

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegisterUserUseCaseTest {

    private lateinit var repository: FakeUsuarioRepository
    private lateinit var useCase: RegisterUserUseCase

    @Before
    fun setUp() {
        repository = FakeUsuarioRepository()
        useCase = RegisterUserUseCase(repository)
    }

    @Test
    fun registroValido_retornaSuccessConIdReal() = runBlocking {
        repository.registerReturnId = 42L

        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals(RegisterUserResult.Success(42L), result)
    }

    @Test
    fun nombre_seNormalizaConTrim() = runBlocking {
        useCase(
            nombre = "  Ana  ",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals("Ana", repository.lastRegisteredUsuario?.nombre)
    }

    @Test
    fun apellido_seNormalizaConTrim() = runBlocking {
        useCase(
            nombre = "Ana",
            apellido = "  Pérez  ",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals("Pérez", repository.lastRegisteredUsuario?.apellido)
    }

    @Test
    fun correo_seNormalizaConTrimYLowercase() = runBlocking {
        useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "  Ana.Perez@Example.COM  ",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals("ana.perez@example.com", repository.lastRegisteredUsuario?.correo)
    }

    @Test
    fun contrasena_noSeModifica() = runBlocking {
        val password = "  Pass Word  "

        useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = password,
            confirmacionContrasena = password,
        )

        assertEquals(password, repository.lastRegisteredUsuario?.contrasena)
    }

    @Test
    fun confirmacionValida_permiteRegistrar() = runBlocking {
        repository.registerReturnId = 7L

        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertTrue(result is RegisterUserResult.Success)
        assertEquals(1, repository.registerCallCount)
    }

    @Test
    fun nombreSoloEspacios_produceError() = runBlocking {
        val result = useCase(
            nombre = "   ",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertValidationContains(result, RegisterValidationError.EMPTY_NAME)
        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun apellidoSoloEspacios_produceError() = runBlocking {
        val result = useCase(
            nombre = "Ana",
            apellido = "   ",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertValidationContains(result, RegisterValidationError.EMPTY_LAST_NAME)
    }

    @Test
    fun correoVacio_produceError() = runBlocking {
        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "   ",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertValidationContains(result, RegisterValidationError.EMPTY_EMAIL)
    }

    @Test
    fun correoFormatoInvalido_produceError() = runBlocking {
        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "no-es-correo",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertValidationContains(result, RegisterValidationError.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun correoConEspacioInterno_produceError() = runBlocking {
        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana perez@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertValidationContains(result, RegisterValidationError.EMAIL_CONTAINS_WHITESPACE)
    }

    @Test
    fun contrasenaVacia_produceError() = runBlocking {
        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "",
            confirmacionContrasena = "",
        )

        assertValidationContains(result, RegisterValidationError.EMPTY_PASSWORD)
    }

    @Test
    fun contrasenaDe7Caracteres_produceError() = runBlocking {
        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "1234567",
            confirmacionContrasena = "1234567",
        )

        assertValidationContains(result, RegisterValidationError.PASSWORD_TOO_SHORT)
    }

    @Test
    fun contrasenaDe8Caracteres_esValida() = runBlocking {
        repository.registerReturnId = 1L
        val password = "12345678"

        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = password,
            confirmacionContrasena = password,
        )

        assertEquals(RegisterUserResult.Success(1L), result)
    }

    @Test
    fun contrasenaDe64Caracteres_esValida() = runBlocking {
        repository.registerReturnId = 1L
        val password = "a".repeat(64)

        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = password,
            confirmacionContrasena = password,
        )

        assertEquals(RegisterUserResult.Success(1L), result)
    }

    @Test
    fun contrasenaDe65Caracteres_produceError() = runBlocking {
        val password = "a".repeat(65)

        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = password,
            confirmacionContrasena = password,
        )

        assertValidationContains(result, RegisterValidationError.PASSWORD_TOO_LONG)
    }

    @Test
    fun confirmacionVacia_produceError() = runBlocking {
        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "",
        )

        assertValidationContains(result, RegisterValidationError.EMPTY_CONFIRMATION)
    }

    @Test
    fun confirmacionDiferente_produceError() = runBlocking {
        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password2",
        )

        assertValidationContains(result, RegisterValidationError.PASSWORD_MISMATCH)
    }

    @Test
    fun multiplesCamposInvalidos_retornanMultiplesErrores() = runBlocking {
        val result = useCase(
            nombre = "   ",
            apellido = "   ",
            correo = "",
            contrasena = "",
            confirmacionContrasena = "otra",
        )

        assertTrue(result is RegisterUserResult.ValidationError)
        val errors = (result as RegisterUserResult.ValidationError).errors
        assertTrue(errors.contains(RegisterValidationError.EMPTY_NAME))
        assertTrue(errors.contains(RegisterValidationError.EMPTY_LAST_NAME))
        assertTrue(errors.contains(RegisterValidationError.EMPTY_EMAIL))
        assertTrue(errors.contains(RegisterValidationError.EMPTY_PASSWORD))
        assertTrue(errors.contains(RegisterValidationError.PASSWORD_MISMATCH))
        assertTrue(errors.size >= 5)
    }

    @Test
    fun repositorioNoSeInvoca_siHayErroresDeValidacion() = runBlocking {
        useCase(
            nombre = "",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals(0, repository.registerCallCount)
        assertNull(repository.lastRegisteredUsuario)
    }

    @Test
    fun idCero_produceUnexpectedError() = runBlocking {
        repository.registerReturnId = 0L

        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals(RegisterUserResult.UnexpectedError, result)
    }

    @Test
    fun idNegativo_produceUnexpectedError() = runBlocking {
        repository.registerReturnId = -1L

        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals(RegisterUserResult.UnexpectedError, result)
    }

    @Test
    fun excepcionDelRepositorio_produceUnexpectedError() = runBlocking {
        repository.registerException = RuntimeException("fallo de persistencia")

        val result = useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals(RegisterUserResult.UnexpectedError, result)
    }

    @Test
    fun repositorioSeInvocaUnaSolaVez_enRegistroValido() = runBlocking {
        useCase(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana@example.com",
            contrasena = "password1",
            confirmacionContrasena = "password1",
        )

        assertEquals(1, repository.registerCallCount)
    }

    private fun assertValidationContains(
        result: RegisterUserResult,
        error: RegisterValidationError,
    ) {
        assertTrue(result is RegisterUserResult.ValidationError)
        assertTrue((result as RegisterUserResult.ValidationError).errors.contains(error))
    }
}
