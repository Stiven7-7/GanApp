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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.R
import com.proyecto.ganapp.domain.usecase.usuario.RegisterValidationError
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing
import com.proyecto.ganapp.ui.features.auth.components.GanAppAuthLayout
import com.proyecto.ganapp.ui.features.auth.components.GanAppFormError
import com.proyecto.ganapp.ui.features.auth.components.GanAppPasswordField
import com.proyecto.ganapp.ui.features.auth.components.GanAppPrimaryButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.registerUiState.collectAsState()
    val lastNameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmationFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val generalErrorMessage = when (uiState.generalError) {
        RegisterGeneralError.DUPLICATE_EMAIL ->
            "Ya existe una cuenta registrada con este correo."
        RegisterGeneralError.UNEXPECTED ->
            "Ocurrió un error inesperado. Intenta de nuevo."
        null -> null
    }

    LaunchedEffect(viewModel) {
        viewModel.registerEvents.collect { event ->
            when (event) {
                RegisterEvent.NavigateToLogin ->
                    onRegisterSuccess()
            }
        }
    }

    GanAppAuthLayout(
        title = "Crear cuenta",
        headerImageRes = R.drawable.logo_register,
        headerImageSize = 104.dp,
        headerImageContentDescription = null,
        subtitle = "Registra tus datos para comenzar a gestionar tu finca."
    ) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onRegisterNameChanged,
            label = { Text("Nombre") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            enabled = !uiState.isLoading,
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { error ->
                { Text(nameErrorMessage(error)) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { lastNameFocusRequester.requestFocus() }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(GanAppSpacing.md))

        OutlinedTextField(
            value = uiState.lastName,
            onValueChange = viewModel::onRegisterLastNameChanged,
            label = { Text("Apellido") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            enabled = !uiState.isLoading,
            isError = uiState.lastNameError != null,
            supportingText = uiState.lastNameError?.let { error ->
                { Text(lastNameErrorMessage(error)) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { emailFocusRequester.requestFocus() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(lastNameFocusRequester)
        )

        Spacer(modifier = Modifier.height(GanAppSpacing.md))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onRegisterEmailChanged,
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
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
        )

        Spacer(modifier = Modifier.height(GanAppSpacing.md))

        GanAppPasswordField(
            value = uiState.password,
            onValueChange = viewModel::onRegisterPasswordChanged,
            label = "Contraseña",
            enabled = !uiState.isLoading,
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { passwordErrorMessage(it) },
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(
                onNext = { confirmationFocusRequester.requestFocus() }
            ),
            focusRequester = passwordFocusRequester
        )

        Spacer(modifier = Modifier.height(GanAppSpacing.md))

        GanAppPasswordField(
            value = uiState.passwordConfirmation,
            onValueChange = viewModel::onRegisterPasswordConfirmationChanged,
            label = "Confirmar contraseña",
            enabled = !uiState.isLoading,
            isError = uiState.confirmationError != null,
            supportingText = uiState.confirmationError?.let { confirmationErrorMessage(it) },
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (!uiState.isLoading) {
                        viewModel.submitRegister()
                    }
                }
            ),
            focusRequester = confirmationFocusRequester
        )

        if (generalErrorMessage != null) {
            Spacer(modifier = Modifier.height(GanAppSpacing.sm))
            GanAppFormError(message = generalErrorMessage)
            Spacer(modifier = Modifier.height(GanAppSpacing.md))
        } else {
            Spacer(modifier = Modifier.height(GanAppSpacing.lg))
        }

        GanAppPrimaryButton(
            label = "Crear cuenta",
            loadingLabel = "Creando cuenta…",
            isLoading = uiState.isLoading,
            enabled = uiState.canSubmit,
            fillMaxWidth = true,
            onClick = viewModel::submitRegister
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
                text = "¿Ya tienes una cuenta?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            TextButton(
                onClick = {
                    viewModel.onRegisterScreenLeaving()
                    onNavigateToLogin()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = GanAppSpacing.xs)
            ) {
                Text(
                    text = "Iniciar sesión",
                    style = MaterialTheme.typography.labelLarge
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
