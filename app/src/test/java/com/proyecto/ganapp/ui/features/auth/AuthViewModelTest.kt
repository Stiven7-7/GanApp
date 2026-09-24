package com.proyecto.ganapp.ui.features.auth

import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.RegisterUserRepositoryResult
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import com.proyecto.ganapp.domain.usecase.usuario.LoginUseCase
import com.proyecto.ganapp.domain.usecase.usuario.LoginValidationError
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserUseCase
import com.proyecto.ganapp.domain.usecase.usuario.RegisterValidationError
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

    @Test
    fun rvm01_estadoInicial_esRegisterUiStateVacio() {
        val state = viewModel.registerUiState.value
        assertEquals(RegisterUiState(), state)
        assertEquals("", state.name)
        assertEquals("", state.lastName)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.passwordConfirmation)
        assertNull(state.nameError)
        assertNull(state.lastNameError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmationError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertTrue(state.canSubmit)
    }

    @Test
    fun rvm02_onRegisterNameChanged_limpiaNameErrorYUnexpectedSinTocarAjenos() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterValidationError.EMPTY_NAME, viewModel.registerUiState.value.nameError)
            assertEquals(RegisterValidationError.EMPTY_LAST_NAME, viewModel.registerUiState.value.lastNameError)

            viewModel.onRegisterNameChanged("Ana")

            val afterName = viewModel.registerUiState.value
            assertEquals("Ana", afterName.name)
            assertNull(afterName.nameError)
            assertEquals(RegisterValidationError.EMPTY_LAST_NAME, afterName.lastNameError)
            assertEquals(RegisterValidationError.EMPTY_EMAIL, afterName.emailError)
            assertEquals(RegisterValidationError.EMPTY_PASSWORD, afterName.passwordError)
            assertEquals(RegisterValidationError.EMPTY_CONFIRMATION, afterName.confirmationError)

            fillValidRegisterForm()
            repository.registerException = IllegalStateException("boom")
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.UNEXPECTED, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterNameChanged("Ana Maria")

            val afterUnexpected = viewModel.registerUiState.value
            assertEquals("Ana Maria", afterUnexpected.name)
            assertNull(afterUnexpected.generalError)
        }

    @Test
    fun rvm03_onRegisterLastNameChanged_limpiaLastNameErrorYUnexpectedSinTocarAjenos() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterValidationError.EMPTY_LAST_NAME, viewModel.registerUiState.value.lastNameError)
            assertEquals(RegisterValidationError.EMPTY_NAME, viewModel.registerUiState.value.nameError)

            viewModel.onRegisterLastNameChanged("Perez")

            val afterLastName = viewModel.registerUiState.value
            assertEquals("Perez", afterLastName.lastName)
            assertNull(afterLastName.lastNameError)
            assertEquals(RegisterValidationError.EMPTY_NAME, afterLastName.nameError)
            assertEquals(RegisterValidationError.EMPTY_EMAIL, afterLastName.emailError)
            assertEquals(RegisterValidationError.EMPTY_PASSWORD, afterLastName.passwordError)
            assertEquals(RegisterValidationError.EMPTY_CONFIRMATION, afterLastName.confirmationError)

            fillValidRegisterForm()
            repository.registerException = IllegalStateException("boom")
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.UNEXPECTED, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterLastNameChanged("Perez Gomez")

            val afterUnexpected = viewModel.registerUiState.value
            assertEquals("Perez Gomez", afterUnexpected.lastName)
            assertNull(afterUnexpected.generalError)
        }

    @Test
    fun rvm04_onRegisterEmailChanged_limpiaEmailErrorDuplicateYUnexpected() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterValidationError.EMPTY_EMAIL, viewModel.registerUiState.value.emailError)

            viewModel.onRegisterEmailChanged("user@example.com")

            val afterEmail = viewModel.registerUiState.value
            assertEquals("user@example.com", afterEmail.email)
            assertNull(afterEmail.emailError)
            assertEquals(RegisterValidationError.EMPTY_NAME, afterEmail.nameError)
            assertEquals(RegisterValidationError.EMPTY_LAST_NAME, afterEmail.lastNameError)

            fillValidRegisterForm()
            repository.registerResult = RegisterUserRepositoryResult.DuplicateEmail
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.DUPLICATE_EMAIL, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterEmailChanged("changed@example.com")
            assertNull(viewModel.registerUiState.value.generalError)
            assertEquals("changed@example.com", viewModel.registerUiState.value.email)

            fillValidRegisterForm()
            repository.registerResult = RegisterUserRepositoryResult.Success(1L)
            repository.registerException = IllegalStateException("boom")
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.UNEXPECTED, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterEmailChanged("other@example.com")
            assertNull(viewModel.registerUiState.value.generalError)
        }

    @Test
    fun rvm05_onRegisterPasswordChanged_limpiaMismatchYUnexpectedNoEmptyConfirmation() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.onRegisterNameChanged("Ana")
            viewModel.onRegisterLastNameChanged("Perez")
            viewModel.onRegisterEmailChanged("user@example.com")
            viewModel.onRegisterPasswordChanged("password12")
            viewModel.onRegisterPasswordConfirmationChanged("different12")
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(
                RegisterValidationError.PASSWORD_MISMATCH,
                viewModel.registerUiState.value.confirmationError,
            )
            assertNull(viewModel.registerUiState.value.passwordError)

            viewModel.onRegisterPasswordChanged("newPassword12")

            val afterMismatch = viewModel.registerUiState.value
            assertEquals("newPassword12", afterMismatch.password)
            assertNull(afterMismatch.passwordError)
            assertNull(afterMismatch.confirmationError)

            viewModel.onRegisterPasswordChanged("password12")
            viewModel.onRegisterPasswordConfirmationChanged("")
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(
                RegisterValidationError.EMPTY_CONFIRMATION,
                viewModel.registerUiState.value.confirmationError,
            )

            viewModel.onRegisterPasswordChanged("changedPass12")

            val afterEmptyConfirmation = viewModel.registerUiState.value
            assertEquals("changedPass12", afterEmptyConfirmation.password)
            assertNull(afterEmptyConfirmation.passwordError)
            assertEquals(
                RegisterValidationError.EMPTY_CONFIRMATION,
                afterEmptyConfirmation.confirmationError,
            )

            fillValidRegisterForm()
            repository.registerException = IllegalStateException("boom")
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.UNEXPECTED, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterPasswordChanged("anotherPass12")
            assertNull(viewModel.registerUiState.value.generalError)
        }

    @Test
    fun rvm06_onRegisterPasswordConfirmationChanged_limpiaConfirmationYUnexpected() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(
                RegisterValidationError.EMPTY_CONFIRMATION,
                viewModel.registerUiState.value.confirmationError,
            )
            assertEquals(RegisterValidationError.EMPTY_PASSWORD, viewModel.registerUiState.value.passwordError)

            viewModel.onRegisterPasswordConfirmationChanged("password12")

            val afterConfirmation = viewModel.registerUiState.value
            assertEquals("password12", afterConfirmation.passwordConfirmation)
            assertNull(afterConfirmation.confirmationError)
            assertEquals(RegisterValidationError.EMPTY_PASSWORD, afterConfirmation.passwordError)

            fillValidRegisterForm()
            repository.registerException = IllegalStateException("boom")
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.UNEXPECTED, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterPasswordConfirmationChanged("password12")
            assertNull(viewModel.registerUiState.value.generalError)
        }

    @Test
    fun rvm07_submitVacio_mapeaErroresDeCampo() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.registerUiState.value
        assertEquals(RegisterValidationError.EMPTY_NAME, state.nameError)
        assertEquals(RegisterValidationError.EMPTY_LAST_NAME, state.lastNameError)
        assertEquals(RegisterValidationError.EMPTY_EMAIL, state.emailError)
        assertEquals(RegisterValidationError.EMPTY_PASSWORD, state.passwordError)
        assertEquals(RegisterValidationError.EMPTY_CONFIRMATION, state.confirmationError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun rvm08_emailInvalido_mapeaInvalidEmailFormat() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onRegisterNameChanged("Ana")
        viewModel.onRegisterLastNameChanged("Perez")
        viewModel.onRegisterEmailChanged("not-an-email")
        viewModel.onRegisterPasswordChanged("password12")
        viewModel.onRegisterPasswordConfirmationChanged("password12")
        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.registerUiState.value
        assertEquals(RegisterValidationError.INVALID_EMAIL_FORMAT, state.emailError)
        assertNull(state.nameError)
        assertNull(state.passwordError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun rvm09_passwordMenorA8_mapeaPasswordTooShort() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onRegisterNameChanged("Ana")
        viewModel.onRegisterLastNameChanged("Perez")
        viewModel.onRegisterEmailChanged("user@example.com")
        viewModel.onRegisterPasswordChanged("short1")
        viewModel.onRegisterPasswordConfirmationChanged("short1")
        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.registerUiState.value
        assertEquals(RegisterValidationError.PASSWORD_TOO_SHORT, state.passwordError)
        assertNull(state.confirmationError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun rvm10_passwordMayorA64_mapeaPasswordTooLong() = runTest(mainDispatcherRule.testDispatcher) {
        val longPassword = "p".repeat(65)
        viewModel.onRegisterNameChanged("Ana")
        viewModel.onRegisterLastNameChanged("Perez")
        viewModel.onRegisterEmailChanged("user@example.com")
        viewModel.onRegisterPasswordChanged(longPassword)
        viewModel.onRegisterPasswordConfirmationChanged(longPassword)
        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.registerUiState.value
        assertEquals(RegisterValidationError.PASSWORD_TOO_LONG, state.passwordError)
        assertNull(state.confirmationError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun rvm11_confirmacionMismatch_mapeaPasswordMismatch() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onRegisterNameChanged("Ana")
        viewModel.onRegisterLastNameChanged("Perez")
        viewModel.onRegisterEmailChanged("user@example.com")
        viewModel.onRegisterPasswordChanged("password12")
        viewModel.onRegisterPasswordConfirmationChanged("different12")
        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.registerUiState.value
        assertEquals(RegisterValidationError.PASSWORD_MISMATCH, state.confirmationError)
        assertNull(state.passwordError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun rvm12_duplicateEmail_mapeaGeneralError() = runTest(mainDispatcherRule.testDispatcher) {
        repository.registerResult = RegisterUserRepositoryResult.DuplicateEmail
        fillValidRegisterForm()
        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.registerUiState.value
        assertEquals(RegisterGeneralError.DUPLICATE_EMAIL, state.generalError)
        assertNull(state.nameError)
        assertNull(state.lastNameError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmationError)
        assertFalse(state.isLoading)
        assertEquals(1, repository.registerCallCount)
    }

    @Test
    fun rvm13_excepcionDelRepositorio_mapeaUnexpected() = runTest(mainDispatcherRule.testDispatcher) {
        repository.registerException = IllegalStateException("boom")
        fillValidRegisterForm()
        viewModel.submitRegister()
        advanceUntilIdle()

        val state = viewModel.registerUiState.value
        assertEquals(RegisterGeneralError.UNEXPECTED, state.generalError)
        assertNull(state.nameError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertFalse(state.isLoading)
        assertEquals(1, repository.registerCallCount)
    }

    @Test
    fun rvm14_registroExitoso_limpiaUiStateYEmiteNavigateToLogin() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.registerResult = RegisterUserRepositoryResult.Success(9L)
            val events = mutableListOf<RegisterEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.registerEvents.collect { events += it }
            }

            fillValidRegisterForm()
            viewModel.submitRegister()
            advanceUntilIdle()

            assertEquals(RegisterUiState(), viewModel.registerUiState.value)
            assertEquals(listOf(RegisterEvent.NavigateToLogin), events)
        }

    @Test
    fun rvm15_eventoOneShot_noSeRepiteEspontaneamente() = runTest(mainDispatcherRule.testDispatcher) {
        repository.registerResult = RegisterUserRepositoryResult.Success(9L)
        val events = mutableListOf<RegisterEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.registerEvents.collect { events += it }
        }

        fillValidRegisterForm()
        viewModel.submitRegister()
        advanceUntilIdle()
        assertEquals(1, events.size)
        assertEquals(RegisterEvent.NavigateToLogin, events.single())
        assertEquals(RegisterUiState(), viewModel.registerUiState.value)

        advanceUntilIdle()
        assertEquals(1, events.size)
        assertNull(viewModel.registerUiState.value.generalError)
        assertFalse(viewModel.registerUiState.value.isLoading)
    }

    @Test
    fun rvm16_normalizacionPermaneceEnDominio() = runTest(mainDispatcherRule.testDispatcher) {
        repository.registerResult = RegisterUserRepositoryResult.Success(3L)
        viewModel.onRegisterNameChanged("  Ana  ")
        viewModel.onRegisterLastNameChanged("  Perez  ")
        viewModel.onRegisterEmailChanged(" USER@EXAMPLE.COM ")
        viewModel.onRegisterPasswordChanged("password12")
        viewModel.onRegisterPasswordConfirmationChanged("password12")
        viewModel.submitRegister()
        advanceUntilIdle()

        val registered = repository.lastRegisteredUser
        assertEquals("Ana", registered?.nombre)
        assertEquals("Perez", registered?.apellido)
        assertEquals("user@example.com", registered?.correo)
        assertEquals(1, repository.registerCallCount)
    }

    @Test
    fun rvm17_passwordNoSeTransforma() = runTest(mainDispatcherRule.testDispatcher) {
        repository.registerResult = RegisterUserRepositoryResult.Success(3L)
        viewModel.onRegisterNameChanged("Ana")
        viewModel.onRegisterLastNameChanged("Perez")
        viewModel.onRegisterEmailChanged("user@example.com")
        viewModel.onRegisterPasswordChanged("  Pass Word12  ")
        viewModel.onRegisterPasswordConfirmationChanged("  Pass Word12  ")
        viewModel.submitRegister()
        advanceUntilIdle()

        assertEquals("  Pass Word12  ", repository.lastRegisteredUser?.contrasena)
    }

    @Test
    fun rvm18_confirmacionValidaPermiteRegistrar() = runTest(mainDispatcherRule.testDispatcher) {
        repository.registerResult = RegisterUserRepositoryResult.Success(5L)
        fillValidRegisterForm()
        viewModel.submitRegister()
        advanceUntilIdle()

        assertEquals(1, repository.registerCallCount)
        assertEquals(RegisterUiState(), viewModel.registerUiState.value)
    }

    @Test
    fun rvm19_onRegisterScreenLeaving_limpiaFormularioCompleto() =
        runTest(mainDispatcherRule.testDispatcher) {
            fillValidRegisterForm()
            repository.registerResult = RegisterUserRepositoryResult.DuplicateEmail
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.DUPLICATE_EMAIL, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterScreenLeaving()

            assertEquals(RegisterUiState(), viewModel.registerUiState.value)
        }

    @Test
    fun rvm20_dobleSubmit_noInvocaRepositorioDosVeces() = runTest(mainDispatcherRule.testDispatcher) {
        repository.registerResult = RegisterUserRepositoryResult.Success(4L)
        fillValidRegisterForm()

        viewModel.submitRegister()
        assertTrue(viewModel.registerUiState.value.isLoading)
        viewModel.submitRegister()
        advanceUntilIdle()

        assertEquals(1, repository.registerCallCount)
    }

    @Test
    fun rvm21_duplicateEmail_editarEmail_limpiaGeneralError() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.registerResult = RegisterUserRepositoryResult.DuplicateEmail
            fillValidRegisterForm()
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.DUPLICATE_EMAIL, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterEmailChanged("changed@example.com")

            val state = viewModel.registerUiState.value
            assertEquals("changed@example.com", state.email)
            assertNull(state.generalError)
        }

    @Test
    fun rvm22_duplicateEmail_editarNombre_noLimpiaDuplicateEmail() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.registerResult = RegisterUserRepositoryResult.DuplicateEmail
            fillValidRegisterForm()
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.DUPLICATE_EMAIL, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterNameChanged("Maria")

            val state = viewModel.registerUiState.value
            assertEquals("Maria", state.name)
            assertEquals(RegisterGeneralError.DUPLICATE_EMAIL, state.generalError)
        }

    @Test
    fun rvm23_duplicateEmail_editarPassword_noLimpiaDuplicateEmail() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.registerResult = RegisterUserRepositoryResult.DuplicateEmail
            fillValidRegisterForm()
            viewModel.submitRegister()
            advanceUntilIdle()
            assertEquals(RegisterGeneralError.DUPLICATE_EMAIL, viewModel.registerUiState.value.generalError)

            viewModel.onRegisterPasswordChanged("newPassword12")

            val state = viewModel.registerUiState.value
            assertEquals("newPassword12", state.password)
            assertEquals(RegisterGeneralError.DUPLICATE_EMAIL, state.generalError)
        }

    private fun fillValidRegisterForm() {
        viewModel.onRegisterNameChanged("Ana")
        viewModel.onRegisterLastNameChanged("Perez")
        viewModel.onRegisterEmailChanged("user@example.com")
        viewModel.onRegisterPasswordChanged("password12")
        viewModel.onRegisterPasswordConfirmationChanged("password12")
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

        var registerResult: RegisterUserRepositoryResult =
            RegisterUserRepositoryResult.Success(1L)
        var registerException: Exception? = null
        var registerCallCount = 0
        var lastRegisteredUser: Usuario? = null

        override suspend fun login(correo: String, contrasena: String): Usuario? {
            loginCallCount += 1
            lastLoginEmail = correo
            lastLoginPassword = contrasena
            loginException?.let { throw it }
            return loginUser
        }

        override suspend fun register(usuario: Usuario): RegisterUserRepositoryResult {
            registerCallCount += 1
            lastRegisteredUser = usuario
            registerException?.let { throw it }
            return registerResult
        }

        override suspend fun getUserById(id: Long): Usuario? {
            error("Unexpected call to getUserById")
        }

        override suspend fun getUsuarioByCorreo(correo: String): Usuario? {
            error("Unexpected call to getUsuarioByCorreo")
        }
    }
}
