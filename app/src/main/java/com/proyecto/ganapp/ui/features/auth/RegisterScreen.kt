package com.proyecto.ganapp.ui.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    val registerState by viewModel.registerState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading = registerState is AuthRegisterState.Loading

    LaunchedEffect(registerState) {
        if (registerState !is AuthRegisterState.Success) return@LaunchedEffect
        onRegisterSuccess()
        viewModel.consumeRegistrationSuccess()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetRegisterState()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FFF9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
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
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de Usuario") },
                placeholder = { Text("Ingresa tu nombre de usuario") },
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853),
                    unfocusedBorderColor = Color(0xFF81C784),
                    focusedLabelColor = Color(0xFF00C853)
                ),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apellido,
                onValueChange = { apellido = it },
                label = { Text("Apellido") },
                placeholder = { Text("Ingresa tu apellido") },
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853),
                    unfocusedBorderColor = Color(0xFF81C784),
                    focusedLabelColor = Color(0xFF00C853)
                ),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo Electrónico") },
                placeholder = { Text("ejemplo@correo.com") },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853),
                    unfocusedBorderColor = Color(0xFF81C784),
                    focusedLabelColor = Color(0xFF00C853)
                ),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))


            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !isLoading,
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🔘 Botón crear cuenta
            Button(
                onClick = {
                    // Temporal: confirmación = contraseña hasta el campo visual en Subfase 1.4.
                    viewModel.register(
                        name = nombre,
                        lastName = apellido,
                        email = correo,
                        password = contrasena,
                        passwordConfirmation = contrasena,
                    )
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text("Crear Cuenta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = registerState) {
                is AuthRegisterState.ValidationError -> {
                    Text(
                        text = formatRegisterValidationErrors(state.errors),
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                AuthRegisterState.UnexpectedError -> {
                    Text(
                        text = "Ocurrió un error inesperado. Intenta de nuevo.",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> Unit
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
                    modifier = Modifier.clickable(enabled = !isLoading) { onNavigateToLogin() }
                )
            }
        }
    }
}

private fun formatRegisterValidationErrors(errors: Set<RegisterValidationError>): String {
    return errors.joinToString(separator = "\n") { error ->
        when (error) {
            RegisterValidationError.EMPTY_NAME -> "El nombre es obligatorio"
            RegisterValidationError.EMPTY_LAST_NAME -> "El apellido es obligatorio"
            RegisterValidationError.EMPTY_EMAIL -> "El correo es obligatorio"
            RegisterValidationError.INVALID_EMAIL_FORMAT -> "El formato del correo no es válido"
            RegisterValidationError.EMAIL_CONTAINS_WHITESPACE -> "El correo no debe contener espacios"
            RegisterValidationError.EMPTY_PASSWORD -> "La contraseña es obligatoria"
            RegisterValidationError.PASSWORD_TOO_SHORT -> "La contraseña debe tener al menos 8 caracteres"
            RegisterValidationError.PASSWORD_TOO_LONG -> "La contraseña supera el máximo permitido"
            RegisterValidationError.EMPTY_CONFIRMATION -> "La confirmación de contraseña es obligatoria"
            RegisterValidationError.PASSWORD_MISMATCH -> "Las contraseñas no coinciden"
        }
    }
}
