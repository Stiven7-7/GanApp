package com.proyecto.ganapp.ui.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.proyecto.ganapp.domain.usecase.usuario.RegisterValidationError

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.registerUiState.collectAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmationPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(viewModel) {
        viewModel.registerEvents.collect { event ->
            when (event) {
                RegisterEvent.NavigateToLogin ->
                    onRegisterSuccess()
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
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // 🌿 Icono del logo
            Image(
                painter = painterResource(id = R.drawable.logo_register), // ícono temporal
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🧭 Título
            Text(
                text = "Crear una Cuenta",
                color = Color(0xFF0A0A0A),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 📋 Campos de entrada
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onRegisterNameChanged,
                label = { Text("Nombre de Usuario") },
                placeholder = { Text("Ingresa tu nombre de usuario") },
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853),
                    unfocusedBorderColor = Color(0xFF81C784),
                    focusedLabelColor = Color(0xFF00C853)
                ),
                enabled = !uiState.isLoading,
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { error ->
                    { Text(nameErrorMessage(error)) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = viewModel::onRegisterLastNameChanged,
                label = { Text("Apellido") },
                placeholder = { Text("Ingresa tu apellido") },
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853),
                    unfocusedBorderColor = Color(0xFF81C784),
                    focusedLabelColor = Color(0xFF00C853)
                ),
                enabled = !uiState.isLoading,
                isError = uiState.lastNameError != null,
                supportingText = uiState.lastNameError?.let { error ->
                    { Text(lastNameErrorMessage(error)) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onRegisterEmailChanged,
                label = { Text("Correo Electrónico") },
                placeholder = { Text("ejemplo@correo.com") },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853),
                    unfocusedBorderColor = Color(0xFF81C784),
                    focusedLabelColor = Color(0xFF00C853)
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
                onValueChange = viewModel::onRegisterPasswordChanged,
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.passwordConfirmation,
                onValueChange = viewModel::onRegisterPasswordConfirmationChanged,
                label = { Text("Confirmar contraseña") },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                visualTransformation = if (confirmationPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { confirmationPasswordVisible = !confirmationPasswordVisible },
                        enabled = !uiState.isLoading,
                    ) {
                        Icon(
                            imageVector = if (confirmationPasswordVisible) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                            contentDescription = if (confirmationPasswordVisible) {
                                "Ocultar confirmación"
                            } else {
                                "Mostrar confirmación"
                            }
                        )
                    }
                },
                enabled = !uiState.isLoading,
                isError = uiState.confirmationError != null,
                supportingText = uiState.confirmationError?.let { error ->
                    { Text(confirmationErrorMessage(error)) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🔘 Botón crear cuenta
            Button(
                onClick = { viewModel.submitRegister() },
                enabled = uiState.canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text("Crear Cuenta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.generalError) {
                RegisterGeneralError.DUPLICATE_EMAIL -> {
                    Text(
                        text = "Ya existe una cuenta registrada con este correo.",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                RegisterGeneralError.UNEXPECTED -> {
                    Text(
                        text = "Ocurrió un error inesperado. Intenta de nuevo.",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                null -> Unit
            }

            // 🔗 Enlace a Login
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "¿Ya tienes una cuenta?",
                    color = Color.Black.copy(alpha = 0.7f),
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Inicia sesión",
                    color = Color(0xFF00C853),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) {
                        viewModel.onRegisterScreenLeaving()
                        onNavigateToLogin()
                    }
                )
            }
        }
    }
}

private fun nameErrorMessage(error: RegisterValidationError): String {
    return when (error) {
        RegisterValidationError.EMPTY_NAME -> "El nombre es obligatorio"
        else -> ""
    }
}

private fun lastNameErrorMessage(error: RegisterValidationError): String {
    return when (error) {
        RegisterValidationError.EMPTY_LAST_NAME -> "El apellido es obligatorio"
        else -> ""
    }
}

private fun emailErrorMessage(error: RegisterValidationError): String {
    return when (error) {
        RegisterValidationError.EMPTY_EMAIL -> "El correo es obligatorio"
        RegisterValidationError.EMAIL_CONTAINS_WHITESPACE -> "El correo no debe contener espacios"
        RegisterValidationError.INVALID_EMAIL_FORMAT -> "El formato del correo no es válido"
        else -> ""
    }
}

private fun passwordErrorMessage(error: RegisterValidationError): String {
    return when (error) {
        RegisterValidationError.EMPTY_PASSWORD -> "La contraseña es obligatoria"
        RegisterValidationError.PASSWORD_TOO_SHORT -> "La contraseña debe tener al menos 8 caracteres"
        RegisterValidationError.PASSWORD_TOO_LONG -> "La contraseña supera el máximo permitido"
        else -> ""
    }
}

private fun confirmationErrorMessage(error: RegisterValidationError): String {
    return when (error) {
        RegisterValidationError.EMPTY_CONFIRMATION -> "La confirmación de contraseña es obligatoria"
        RegisterValidationError.PASSWORD_MISMATCH -> "Las contraseñas no coinciden"
        else -> ""
    }
}
