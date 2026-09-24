package com.proyecto.ganapp.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.usecase.usuario.LoginResult
import com.proyecto.ganapp.domain.usecase.usuario.LoginUseCase
import com.proyecto.ganapp.domain.usecase.usuario.LoginValidationError
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserResult
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserUseCase
import com.proyecto.ganapp.domain.usecase.usuario.RegisterValidationError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
) : ViewModel() {

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _loginEvents = Channel<LoginEvent>(Channel.BUFFERED)
    val loginEvents: Flow<LoginEvent> = _loginEvents.receiveAsFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    private val _registerEvents = Channel<RegisterEvent>(Channel.BUFFERED)
    val registerEvents: Flow<RegisterEvent> = _registerEvents.receiveAsFlow()

    fun onLoginEmailChanged(value: String) {
        _loginUiState.update {
            it.copy(
                email = value,
                emailError = null,
                generalError = null,
            )
        }
    }

    fun onLoginPasswordChanged(value: String) {
        _loginUiState.update {
            it.copy(
                password = value,
                passwordError = null,
                generalError = null,
            )
        }
    }

    fun submitLogin() {
        if (_loginUiState.value.isLoading) return

        _loginUiState.update {
            it.copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                generalError = null,
            )
        }

        viewModelScope.launch {
            val current = _loginUiState.value
            when (val result = loginUseCase(current.email, current.password)) {
                is LoginResult.ValidationError -> {
                    _loginUiState.update {
                        it.copy(
                            isLoading = false,
                            emailError = result.errors.emailError(),
                            passwordError = result.errors.passwordError(),
                            generalError = null,
                        )
                    }
                }
                LoginResult.InvalidCredentials -> {
                    _loginUiState.update {
                        it.copy(
                            isLoading = false,
                            emailError = null,
                            passwordError = null,
                            generalError = LoginGeneralError.INVALID_CREDENTIALS,
                        )
                    }
                }
                LoginResult.UnexpectedError -> {
                    _loginUiState.update {
                        it.copy(
                            isLoading = false,
                            emailError = null,
                            passwordError = null,
                            generalError = LoginGeneralError.UNEXPECTED,
                        )
                    }
                }
                is LoginResult.Success -> {
                    _loginUiState.value = LoginUiState()
                    _loginEvents.send(
                        LoginEvent.NavigateToHome(
                            userId = result.authenticatedUser.idUsuario,
                        )
                    )
                }
            }
        }
    }

    fun onLoginScreenLeaving() {
        _loginUiState.update { current ->
            LoginUiState(email = current.email)
        }
    }

    fun onRegisterNameChanged(value: String) {
        _registerUiState.update {
            it.copy(
                name = value,
                nameError = null,
                generalError = it.generalError.clearUnexpectedOnly(),
            )
        }
    }

    fun onRegisterLastNameChanged(value: String) {
        _registerUiState.update {
            it.copy(
                lastName = value,
                lastNameError = null,
                generalError = it.generalError.clearUnexpectedOnly(),
            )
        }
    }

    fun onRegisterEmailChanged(value: String) {
        _registerUiState.update {
            it.copy(
                email = value,
                emailError = null,
                generalError = null,
            )
        }
    }

    fun onRegisterPasswordChanged(value: String) {
        _registerUiState.update {
            it.copy(
                password = value,
                passwordError = null,
                confirmationError = if (it.confirmationError == RegisterValidationError.PASSWORD_MISMATCH) {
                    null
                } else {
                    it.confirmationError
                },
                generalError = it.generalError.clearUnexpectedOnly(),
            )
        }
    }

    fun onRegisterPasswordConfirmationChanged(value: String) {
        _registerUiState.update {
            it.copy(
                passwordConfirmation = value,
                confirmationError = null,
                generalError = it.generalError.clearUnexpectedOnly(),
            )
        }
    }

    fun submitRegister() {
        if (_registerUiState.value.isLoading) return

        _registerUiState.update {
            it.copy(
                isLoading = true,
                nameError = null,
                lastNameError = null,
                emailError = null,
                passwordError = null,
                confirmationError = null,
                generalError = null,
            )
        }

        viewModelScope.launch {
            val current = _registerUiState.value
            when (
                val result = registerUserUseCase(
                    nombre = current.name,
                    apellido = current.lastName,
                    correo = current.email,
                    contrasena = current.password,
                    confirmacionContrasena = current.passwordConfirmation,
                )
            ) {
                is RegisterUserResult.ValidationError -> {
                    _registerUiState.update {
                        it.copy(
                            isLoading = false,
                            nameError = result.errors.nameError(),
                            lastNameError = result.errors.lastNameError(),
                            emailError = result.errors.emailError(),
                            passwordError = result.errors.passwordError(),
                            confirmationError = result.errors.confirmationError(),
                            generalError = null,
                        )
                    }
                }
                RegisterUserResult.DuplicateEmail -> {
                    _registerUiState.update {
                        it.copy(
                            isLoading = false,
                            nameError = null,
                            lastNameError = null,
                            emailError = null,
                            passwordError = null,
                            confirmationError = null,
                            generalError = RegisterGeneralError.DUPLICATE_EMAIL,
                        )
                    }
                }
                RegisterUserResult.UnexpectedError -> {
                    _registerUiState.update {
                        it.copy(
                            isLoading = false,
                            nameError = null,
                            lastNameError = null,
                            emailError = null,
                            passwordError = null,
                            confirmationError = null,
                            generalError = RegisterGeneralError.UNEXPECTED,
                        )
                    }
                }
                is RegisterUserResult.Success -> {
                    _registerUiState.value = RegisterUiState()
                    _registerEvents.send(RegisterEvent.NavigateToLogin)
                }
            }
        }
    }

    fun onRegisterScreenLeaving() {
        _registerUiState.value = RegisterUiState()
    }

    fun logout() {
        _loginUiState.value = LoginUiState()
        _registerUiState.value = RegisterUiState()
    }

    private fun Set<LoginValidationError>.emailError(): LoginValidationError? {
        return when {
            LoginValidationError.EMPTY_EMAIL in this -> LoginValidationError.EMPTY_EMAIL
            LoginValidationError.EMAIL_CONTAINS_WHITESPACE in this ->
                LoginValidationError.EMAIL_CONTAINS_WHITESPACE
            LoginValidationError.INVALID_EMAIL_FORMAT in this ->
                LoginValidationError.INVALID_EMAIL_FORMAT
            else -> null
        }
    }

    private fun Set<LoginValidationError>.passwordError(): LoginValidationError? {
        return when {
            LoginValidationError.EMPTY_PASSWORD in this -> LoginValidationError.EMPTY_PASSWORD
            LoginValidationError.PASSWORD_TOO_LONG in this -> LoginValidationError.PASSWORD_TOO_LONG
            else -> null
        }
    }

    private fun Set<RegisterValidationError>.nameError(): RegisterValidationError? {
        return if (RegisterValidationError.EMPTY_NAME in this) {
            RegisterValidationError.EMPTY_NAME
        } else {
            null
        }
    }

    private fun Set<RegisterValidationError>.lastNameError(): RegisterValidationError? {
        return if (RegisterValidationError.EMPTY_LAST_NAME in this) {
            RegisterValidationError.EMPTY_LAST_NAME
        } else {
            null
        }
    }

    private fun Set<RegisterValidationError>.emailError(): RegisterValidationError? {
        return when {
            RegisterValidationError.EMPTY_EMAIL in this -> RegisterValidationError.EMPTY_EMAIL
            RegisterValidationError.EMAIL_CONTAINS_WHITESPACE in this ->
                RegisterValidationError.EMAIL_CONTAINS_WHITESPACE
            RegisterValidationError.INVALID_EMAIL_FORMAT in this ->
                RegisterValidationError.INVALID_EMAIL_FORMAT
            else -> null
        }
    }

    private fun Set<RegisterValidationError>.passwordError(): RegisterValidationError? {
        return when {
            RegisterValidationError.EMPTY_PASSWORD in this -> RegisterValidationError.EMPTY_PASSWORD
            RegisterValidationError.PASSWORD_TOO_SHORT in this ->
                RegisterValidationError.PASSWORD_TOO_SHORT
            RegisterValidationError.PASSWORD_TOO_LONG in this ->
                RegisterValidationError.PASSWORD_TOO_LONG
            else -> null
        }
    }

    private fun Set<RegisterValidationError>.confirmationError(): RegisterValidationError? {
        return when {
            RegisterValidationError.EMPTY_CONFIRMATION in this ->
                RegisterValidationError.EMPTY_CONFIRMATION
            RegisterValidationError.PASSWORD_MISMATCH in this ->
                RegisterValidationError.PASSWORD_MISMATCH
            else -> null
        }
    }

    private fun RegisterGeneralError?.clearUnexpectedOnly(): RegisterGeneralError? {
        return if (this == RegisterGeneralError.UNEXPECTED) null else this
    }
}
