package com.proyecto.ganapp.ui.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.R
import com.proyecto.ganapp.domain.usecase.usuario.LoginValidationError
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing
import com.proyecto.ganapp.ui.features.auth.components.GanAppAuthLayout
import com.proyecto.ganapp.ui.features.auth.components.GanAppFormError
import com.proyecto.ganapp.ui.features.auth.components.GanAppPasswordField
import com.proyecto.ganapp.ui.features.auth.components.GanAppPrimaryButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.loginUiState.collectAsState()
    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val generalErrorMessage = when (uiState.generalError) {
        LoginGeneralError.INVALID_CREDENTIALS -> "Correo o contraseña incorrectos"
        LoginGeneralError.UNEXPECTED -> "Ocurrió un error inesperado. Intenta de nuevo."
        null -> null
    }

    GanAppAuthLayout(
        title = "Inicia sesión",
        headerImageRes = R.drawable.logo_app,
        headerImageSize = 128.dp,
        headerImageContentDescription = "GANAPP",
        subtitle = "Gestiona tu finca desde un solo lugar."
    ) {
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onLoginEmailChanged,
            label = { Text("Correo electrónico") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            enabled = !uiState.isLoading,
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { error ->
                { Text(emailErrorMessage(error)) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(GanAppSpacing.md))

        GanAppPasswordField(
            value = uiState.password,
            onValueChange = viewModel::onLoginPasswordChanged,
            label = "Contraseña",
            enabled = !uiState.isLoading,
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { passwordErrorMessage(it) },
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (!uiState.isLoading) {
                        viewModel.submitLogin()
                    }
                }
            ),
            focusRequester = passwordFocusRequester
        )

        if (generalErrorMessage != null) {
            Spacer(modifier = Modifier.height(GanAppSpacing.sm))
            GanAppFormError(message = generalErrorMessage)
            Spacer(modifier = Modifier.height(GanAppSpacing.md))
        } else {
            Spacer(modifier = Modifier.height(GanAppSpacing.lg))
        }

        GanAppPrimaryButton(
            label = "Iniciar sesión",
            loadingLabel = "Iniciando sesión…",
            isLoading = uiState.isLoading,
            enabled = uiState.canSubmit,
            fillMaxWidth = true,
            onClick = viewModel::submitLogin
        )

        Spacer(modifier = Modifier.height(GanAppSpacing.sm))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                GanAppSpacing.xxs,
                Alignment.CenterHorizontally
            ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¿No tienes una cuenta?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            TextButton(
                onClick = {
                    viewModel.onLoginScreenLeaving()
                    onNavigateToRegister()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = GanAppSpacing.xs)
            ) {
                Text(
                    text = "Crear cuenta",
                    style = MaterialTheme.typography.labelLarge
                )
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
