package com.proyecto.ganapp.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.usecase.usuario.LoginResult
import com.proyecto.ganapp.domain.usecase.usuario.LoginUseCase
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserResult
import com.proyecto.ganapp.domain.usecase.usuario.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthLoginState>(AuthLoginState.Idle)
    val loginState: StateFlow<AuthLoginState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthRegisterState>(AuthRegisterState.Idle)
    val registerState: StateFlow<AuthRegisterState> = _registerState.asStateFlow()

    fun login(email: String, password: String) {
        if (_loginState.value is AuthLoginState.Loading) return

        _loginState.value = AuthLoginState.Loading

        viewModelScope.launch {
            _loginState.value = when (val result = loginUseCase(email, password)) {
                is LoginResult.Success -> AuthLoginState.Success(result.authenticatedUser)
                is LoginResult.ValidationError -> AuthLoginState.ValidationError(result.errors)
                LoginResult.InvalidCredentials -> AuthLoginState.InvalidCredentials
                LoginResult.UnexpectedError -> AuthLoginState.UnexpectedError
            }
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

    /**
     * Reinicia el estado de login tras navegar a Home para evitar reprocesar Success.
     */
    fun consumeLoginSuccess() {
        _loginState.value = AuthLoginState.Idle
    }

    fun resetLoginState() {
        if (_loginState.value !is AuthLoginState.Loading) {
            _loginState.value = AuthLoginState.Idle
        }
    }

    fun resetRegisterState() {
        if (_registerState.value !is AuthRegisterState.Loading) {
            _registerState.value = AuthRegisterState.Idle
        }
    }

    fun logout() {
        _loginState.value = AuthLoginState.Idle
        _registerState.value = AuthRegisterState.Idle
    }
}
