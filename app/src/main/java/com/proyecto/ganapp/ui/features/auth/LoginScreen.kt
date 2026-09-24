package com.proyecto.ganapp.ui.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.proyecto.ganapp.domain.usecase.usuario.LoginValidationError

@Composable
fun LoginScreen(
    onLoginSuccess: (Long) -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.loginUiState.collectAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.loginEvents.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> onLoginSuccess(event.userId)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FFF9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = "Logo",
                modifier = Modifier.size(250.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Iniciar Sesión",
                color = Color(0xFF0A0A0A),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onLoginEmailChanged,
                label = { Text("Correo electrónico") },
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853)
                ),
                enabled = !uiState.isLoading,
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { error ->
                    { Text(emailErrorMessage(error)) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onLoginPasswordChanged,
                label = { Text("Contraseña") },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !uiState.isLoading,
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                enabled = !uiState.isLoading,
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { error ->
                    { Text(passwordErrorMessage(error)) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.submitLogin() },
                enabled = uiState.canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text("Iniciar Sesión", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¿No tienes cuenta? Regístrate",
                color = Color(0xFF00C853),
                modifier = Modifier.clickable(enabled = !uiState.isLoading) {
                    viewModel.onLoginScreenLeaving()
                    onNavigateToRegister()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (uiState.generalError) {
                LoginGeneralError.INVALID_CREDENTIALS -> {
                    Text(
                        text = "Correo o contraseña incorrectos",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
                LoginGeneralError.UNEXPECTED -> {
                    Text(
                        text = "Ocurrió un error inesperado. Intenta de nuevo.",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
                null -> Unit
            }
        }
    }
}

private fun emailErrorMessage(error: LoginValidationError): String {
    return when (error) {
        LoginValidationError.EMPTY_EMAIL -> "El correo es obligatorio"
        LoginValidationError.EMAIL_CONTAINS_WHITESPACE -> "El correo no debe contener espacios"
        LoginValidationError.INVALID_EMAIL_FORMAT -> "El formato del correo no es válido"
        else -> ""
    }
}

private fun passwordErrorMessage(error: LoginValidationError): String {
    return when (error) {
        LoginValidationError.EMPTY_PASSWORD -> "La contraseña es obligatoria"
        LoginValidationError.PASSWORD_TOO_LONG -> "La contraseña supera el máximo permitido"
        else -> ""
    }
}
