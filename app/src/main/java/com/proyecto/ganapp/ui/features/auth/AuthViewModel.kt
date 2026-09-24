package com.proyecto.ganapp.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.usecase.usuario.LoginResult
import com.proyecto.ganapp.domain.usecase.usuario.LoginUseCase
import com.proyecto.ganapp.domain.usecase.usuario.LoginValidationError
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserResult
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserUseCase
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

    private val _registerState = MutableStateFlow<AuthRegisterState>(AuthRegisterState.Idle)
    val registerState: StateFlow<AuthRegisterState> = _registerState.asStateFlow()

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

    fun register(
        name: String,
        lastName: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ) {
        if (_registerState.value is AuthRegisterState.Loading) return

        _registerState.value = AuthRegisterState.Loading

        viewModelScope.launch {
            _registerState.value = when (
                val result = registerUserUseCase(
                    nombre = name,
                    apellido = lastName,
                    correo = email,
                    contrasena = password,
                    confirmacionContrasena = passwordConfirmation,
                )
            ) {
                is RegisterUserResult.Success -> AuthRegisterState.Success(result.userId)
                is RegisterUserResult.ValidationError ->
                    AuthRegisterState.ValidationError(result.errors)
                RegisterUserResult.DuplicateEmail -> AuthRegisterState.DuplicateEmail
                RegisterUserResult.UnexpectedError -> AuthRegisterState.UnexpectedError
            }
        }
    }

    /**
     * Reinicia el estado de registro tras navegar a Login (adaptación temporal Bloque 3).
     */
    fun consumeRegistrationSuccess() {
        _registerState.value = AuthRegisterState.Idle
    }

    fun resetRegisterState() {
        if (_registerState.value !is AuthRegisterState.Loading) {
            _registerState.value = AuthRegisterState.Idle
        }
    }

    fun logout() {
        _loginUiState.value = LoginUiState()
        _registerState.value = AuthRegisterState.Idle
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
}
