package com.proyecto.ganapp.ui.features.animals

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.RegisterAnimalValidationError
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterAnimalScreen(
    idUsuario: Long,
    onAnimalSaved: () -> Unit,
    viewModel: AnimalsViewModel = hiltViewModel()
) {
    var especie by remember { mutableStateOf<EspecieAnimal?>(null) }
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<String?>(null) }
    var localEdadError by remember { mutableStateOf<String?>(null) }
    var localPesoError by remember { mutableStateOf<String?>(null) }

    val registrationState by viewModel.registrationUiState.collectAsState()
    val isLoading = registrationState is AnimalRegistrationUiState.Loading
    val inputsEnabled = !isLoading && registrationState !is AnimalRegistrationUiState.Success

    LaunchedEffect(registrationState) {
        if (registrationState is AnimalRegistrationUiState.Success) {
            onAnimalSaved()
            viewModel.consumeRegistrationSuccess()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fotoUri = uri?.toString()
    }

    val validationErrors = (registrationState as? AnimalRegistrationUiState.ValidationError)
        ?.errors
        .orEmpty()
    val speciesError = validationErrors
        .filter {
            it == RegisterAnimalValidationError.SPECIES_REQUIRED ||
                it == RegisterAnimalValidationError.SPECIES_NOT_ALLOWED
        }
        .sortedBy { it.ordinal }
        .joinToString(separator = "\n") { error -> validationMessage(error) }
        .ifBlank { null }
    val nameError = validationErrors
        .firstOrNull { it == RegisterAnimalValidationError.NAME_REQUIRED }
        ?.let { validationMessage(it) }
    val ageError = localEdadError ?: validationErrors
        .firstOrNull { it == RegisterAnimalValidationError.INVALID_AGE }
        ?.let { validationMessage(it) }
    val weightError = localPesoError ?: validationErrors
        .firstOrNull { it == RegisterAnimalValidationError.INVALID_WEIGHT }
        ?.let { validationMessage(it) }
    val formError = when (val state = registrationState) {
        is AnimalRegistrationUiState.Failure ->
            "No fue posible registrar el animal. Intenta nuevamente."
        is AnimalRegistrationUiState.ValidationError ->
            state.errors
                .filter { it == RegisterAnimalValidationError.INVALID_USER }
                .sortedBy { it.ordinal }
                .joinToString(separator = "\n") { error -> validationMessage(error) }
                .ifBlank { null }
        AnimalRegistrationUiState.Idle,
        AnimalRegistrationUiState.Loading,
        is AnimalRegistrationUiState.Success,
        -> null
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Registrar Animal", textAlign = TextAlign.Center) },
                expandedHeight = if (LocalDensity.current.fontScale >= 1.5f) {
                    GanAppSpacing.xxl * 3
                } else {
                    TopAppBarDefaults.TopAppBarExpandedHeight
                },
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(GanAppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            AnimalFormPhoto(
                fotoUri = fotoUri,
                fallbackSpecies = especie,
                enabled = inputsEnabled,
                pickLabel = if (fotoUri == null) "Seleccionar foto" else "Cambiar foto",
                showRemove = fotoUri != null,
                onPickPhoto = { launcher.launch("image/*") },
                onRemovePhoto = { fotoUri = null },
            )
            AnimalFormSectionTitle(text = "Especie")
            AnimalSpeciesSelector(
                selected = especie,
                enabled = inputsEnabled,
                onSelected = { especie = it },
            )
            if (speciesError != null) {
                Text(
                    text = speciesError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            AnimalFormSectionTitle(text = "Información")
            AnimalFormTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = "Nombre del animal",
                placeholder = "Ej: Manchita",
                enabled = inputsEnabled,
                error = nameError,
            )
            AnimalFormTextField(
                value = edad,
                onValueChange = {
                    edad = it
                    localEdadError = null
                },
                label = "Edad (años)",
                enabled = inputsEnabled,
                error = ageError,
                keyboardType = KeyboardType.Number,
            )
            AnimalFormTextField(
                value = peso,
                onValueChange = {
                    peso = it
                    localPesoError = null
                },
                label = "Peso (kg)",
                enabled = inputsEnabled,
                error = weightError,
                keyboardType = KeyboardType.Number,
            )
            AnimalFormTextField(
                value = color,
                onValueChange = { color = it },
                label = "Color",
                enabled = inputsEnabled,
                error = null,
            )
            if (formError != null) {
                Text(
                    text = formError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = {
                    if (!inputsEnabled) return@Button
                    val parsedEdad = parseOptionalInt(edad)
                    val parsedPeso = parseOptionalFloat(peso)
                    val edadInvalida = edad.isNotBlank() && parsedEdad == null
                    val pesoInvalido = peso.isNotBlank() && parsedPeso == null
                    localEdadError = if (edadInvalida) "La edad no es válida." else null
                    localPesoError = if (pesoInvalido) "El peso no es válido." else null
                    if (edadInvalida || pesoInvalido) return@Button
                    viewModel.registerAnimal(
                        especie = especie,
                        nombre = nombre,
                        edad = parsedEdad,
                        peso = parsedPeso,
                        color = color,
                        fotoUri = fotoUri,
                        idUsuario = idUsuario,
                    )
                },
                enabled = inputsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AnimalFormPrimaryButtonMinHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.size(GanAppSpacing.xs))
                }
                Text(
                    text = "Guardar en dispositivo",
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun parseOptionalInt(raw: String): Int? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    return text.toIntOrNull()
}

private fun parseOptionalFloat(raw: String): Float? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    return text.toFloatOrNull()
}

private fun validationMessage(error: RegisterAnimalValidationError): String {
    return when (error) {
        RegisterAnimalValidationError.SPECIES_REQUIRED -> "Seleccione una especie."
        RegisterAnimalValidationError.SPECIES_NOT_ALLOWED -> "La especie seleccionada no es válida."
        RegisterAnimalValidationError.NAME_REQUIRED -> "El nombre es obligatorio."
        RegisterAnimalValidationError.INVALID_AGE -> "La edad no es válida."
        RegisterAnimalValidationError.INVALID_WEIGHT -> "El peso no es válido."
        RegisterAnimalValidationError.INVALID_USER -> "No se pudo identificar el usuario."
    }
}
