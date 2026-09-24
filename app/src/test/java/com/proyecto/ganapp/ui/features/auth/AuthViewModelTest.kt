package com.proyecto.ganapp.ui.features.auth

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.RegisterUserRepositoryResult
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import com.proyecto.ganapp.domain.usecase.usuario.LoginUseCase
import com.proyecto.ganapp.domain.usecase.usuario.LoginValidationError
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserUseCase
import com.proyecto.ganapp.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeUsuarioRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        repository = FakeUsuarioRepository()
        viewModel = AuthViewModel(
            loginUseCase = LoginUseCase(repository),
            registerUserUseCase = RegisterUserUseCase(repository),
        )
    }

    @Test
    fun lvm01_estadoInicial_esLoginUiStateVacio() {
        val state = viewModel.loginUiState.value
        assertEquals(LoginUiState(), state)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertTrue(state.canSubmit)
    }

    @Test
    fun lvm02_onLoginEmailChanged_limpiaEmailYGeneralSinTocarPasswordError() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.submitLogin()
        advanceUntilIdle()
        assertEquals(LoginValidationError.EMPTY_EMAIL, viewModel.loginUiState.value.emailError)
        assertEquals(LoginValidationError.EMPTY_PASSWORD, viewModel.loginUiState.value.passwordError)

        viewModel.onLoginEmailChanged("user@example.com")

        val state = viewModel.loginUiState.value
        assertEquals("user@example.com", state.email)
        assertNull(state.emailError)
        assertNull(state.generalError)
        assertEquals(LoginValidationError.EMPTY_PASSWORD, state.passwordError)
    }

    @Test
    fun lvm03_onLoginPasswordChanged_limpiaPasswordYGeneralSinTocarEmailError() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.submitLogin()
        advanceUntilIdle()
        assertEquals(LoginValidationError.EMPTY_EMAIL, viewModel.loginUiState.value.emailError)
        assertEquals(LoginValidationError.EMPTY_PASSWORD, viewModel.loginUiState.value.passwordError)

        viewModel.onLoginPasswordChanged("password12")

        val state = viewModel.loginUiState.value
        assertEquals("password12", state.password)
        assertNull(state.passwordError)
        assertNull(state.generalError)
        assertEquals(LoginValidationError.EMPTY_EMAIL, state.emailError)
    }

    @Test
    fun lvm04_submitVacio_mapeaErroresDeCampo() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.loginUiState.value
        assertEquals(LoginValidationError.EMPTY_EMAIL, state.emailError)
        assertEquals(LoginValidationError.EMPTY_PASSWORD, state.passwordError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertEquals(0, repository.loginCallCount)
    }

    @Test
    fun lvm05_emailInvalido_mapeaInvalidEmailFormat() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onLoginEmailChanged("not-an-email")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.loginUiState.value
        assertEquals(LoginValidationError.INVALID_EMAIL_FORMAT, state.emailError)
        assertNull(state.passwordError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
    }

    @Test
    fun lvm06_passwordMayorA64_mapeaPasswordTooLong() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("p".repeat(65))
        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.loginUiState.value
        assertEquals(LoginValidationError.PASSWORD_TOO_LONG, state.passwordError)
        assertNull(state.emailError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
    }

    @Test
    fun lvm07_credencialesInvalidas_mapeaGeneralError() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginUser = null
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.loginUiState.value
        assertEquals(LoginGeneralError.INVALID_CREDENTIALS, state.generalError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun lvm08_excepcionDelRepositorio_mapeaUnexpected() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginException = IllegalStateException("boom")
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()

        val state = viewModel.loginUiState.value
        assertEquals(LoginGeneralError.UNEXPECTED, state.generalError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun lvm09_loginExitoso_limpiaUiStateYEmiteNavigateToHome() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginUser = usuario(id = 7L)
        val events = mutableListOf<LoginEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.loginEvents.collect { events += it }
        }

        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()

        assertEquals(LoginUiState(), viewModel.loginUiState.value)
        assertEquals(listOf(LoginEvent.NavigateToHome(userId = 7L)), events)
    }

    @Test
    fun lvm10_eventoOneShot_noSeRepiteEspontaneamente() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginUser = usuario(id = 7L)
        val events = mutableListOf<LoginEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.loginEvents.collect { events += it }
        }

        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()
        assertEquals(1, events.size)
        assertEquals(LoginUiState(), viewModel.loginUiState.value)

        advanceUntilIdle()
        assertEquals(1, events.size)
        assertNull(viewModel.loginUiState.value.generalError)
        assertFalse(viewModel.loginUiState.value.isLoading)
    }

    @Test
    fun lvm11_normalizacionPermaneceEnDominio() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginUser = usuario(id = 3L)
        viewModel.onLoginEmailChanged(" USER@EXAMPLE.COM ")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()

        assertEquals("user@example.com", repository.lastLoginEmail)
        assertEquals("password12", repository.lastLoginPassword)
        assertEquals(1, repository.loginCallCount)
    }

    @Test
    fun lvm12_onLoginScreenLeaving_conservaEmailYLimpiaSecretosYErrores() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginUser = null
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()
        assertEquals(LoginGeneralError.INVALID_CREDENTIALS, viewModel.loginUiState.value.generalError)

        viewModel.onLoginScreenLeaving()

        val state = viewModel.loginUiState.value
        assertEquals("user@example.com", state.email)
        assertEquals("", state.password)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
    }

    @Test
    fun lvm13_dobleSubmit_noInvocaRepositorioDosVeces() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginUser = usuario(id = 4L)
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password12")

        viewModel.submitLogin()
        assertTrue(viewModel.loginUiState.value.isLoading)
        viewModel.submitLogin()
        advanceUntilIdle()

        assertEquals(1, repository.loginCallCount)
    }

    @Test
    fun lvm14_invalidCredentials_editarEmail_limpiaGeneralError() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginUser = null
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()
        assertEquals(LoginGeneralError.INVALID_CREDENTIALS, viewModel.loginUiState.value.generalError)

        viewModel.onLoginEmailChanged("changed@example.com")

        val state = viewModel.loginUiState.value
        assertEquals("changed@example.com", state.email)
        assertNull(state.generalError)
    }

    @Test
    fun lvm15_invalidCredentials_editarPassword_limpiaGeneralError() = runTest(mainDispatcherRule.testDispatcher) {
        repository.loginUser = null
        viewModel.onLoginEmailChanged("user@example.com")
        viewModel.onLoginPasswordChanged("password12")
        viewModel.submitLogin()
        advanceUntilIdle()
        assertEquals(LoginGeneralError.INVALID_CREDENTIALS, viewModel.loginUiState.value.generalError)

        viewModel.onLoginPasswordChanged("newPassword12")

        val state = viewModel.loginUiState.value
        assertEquals("newPassword12", state.password)
        assertNull(state.generalError)
    }

    private fun usuario(id: Long): Usuario {
        return Usuario(
            idUsuario = id,
            nombre = "Ana",
            apellido = "Perez",
            correo = "user@example.com",
            contrasena = "unused",
        )
    }

    private class FakeUsuarioRepository : UsuarioRepository {
        var loginUser: Usuario? = null
        var loginException: Exception? = null
        var loginCallCount = 0
        var lastLoginEmail: String? = null
        var lastLoginPassword: String? = null

        override suspend fun login(correo: String, contrasena: String): Usuario? {
            loginCallCount += 1
            lastLoginEmail = correo
            lastLoginPassword = contrasena
            loginException?.let { throw it }
            return loginUser
        }

        override suspend fun register(usuario: Usuario): RegisterUserRepositoryResult {
            error("Unexpected call to register")
        }

        override suspend fun getUserById(id: Long): Usuario? {
            error("Unexpected call to getUserById")
        }

        override suspend fun getUsuarioByCorreo(correo: String): Usuario? {
            error("Unexpected call to getUsuarioByCorreo")
        }
    }
}
