package com.proyecto.ganapp.ui.features.animals

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.RegisterAnimalValidationError
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

private val registerSpeciesOptions = listOf(
    EspecieAnimal.VACA to "Vaca",
    EspecieAnimal.CERDO to "Cerdo",
    EspecieAnimal.OVEJA to "Oveja",
)

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
                colors = ganAppTopAppBarColors(),
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
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
        ) {
            RegisterAnimalPhoto(
                fotoUri = fotoUri,
                fallbackSpecies = especie,
                enabled = inputsEnabled,
                onPickPhoto = { launcher.launch("image/*") },
                onRemovePhoto = { fotoUri = null },
            )
            RegisterSpeciesSelector(
                selected = especie,
                enabled = inputsEnabled,
                error = speciesError,
                onSelected = { especie = it },
            )
            RegisterInformationCard(
                nombre = nombre,
                onNombreChange = { nombre = it },
                edad = edad,
                onEdadChange = {
                    edad = it
                    localEdadError = null
                },
                peso = peso,
                onPesoChange = {
                    peso = it
                    localPesoError = null
                },
                color = color,
                onColorChange = { color = it },
                enabled = inputsEnabled,
                nameError = nameError,
                ageError = ageError,
                weightError = weightError,
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
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large,
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
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegisterAnimalPhoto(
    fotoUri: String?,
    fallbackSpecies: EspecieAnimal?,
    enabled: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    var photoFailed by remember(fotoUri) { mutableStateOf(false) }
    val showPhoto = !fotoUri.isNullOrBlank() && !photoFailed
    val frameModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(4f / 3f)
        .clip(MaterialTheme.shapes.large)

    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
        Text(
            text = "Foto del animal",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (showPhoto) {
            AsyncImage(
                model = fotoUri,
                contentDescription = null,
                modifier = frameModifier,
                contentScale = ContentScale.Crop,
                onError = { photoFailed = true },
            )
        } else {
            val fallbackRes = fallbackSpecies?.let(::getAnimalImageRes)
            Surface(
                modifier = frameModifier,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (fallbackRes != null) {
                        Image(
                            painter = painterResource(id = fallbackRes),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(GanAppSpacing.lg),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(GanAppSpacing.xxl),
                        )
                    }
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
        ) {
            OutlinedButton(
                onClick = onPickPhoto,
                enabled = enabled,
                modifier = Modifier.heightIn(min = GanAppSpacing.xxl),
            ) {
                Text(
                    text = if (fotoUri == null) "Seleccionar foto" else "Cambiar foto",
                    textAlign = TextAlign.Center,
                )
            }
            if (fotoUri != null) {
                OutlinedButton(
                    onClick = onRemovePhoto,
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = GanAppSpacing.xxl),
                ) {
                    Text(text = "Quitar foto", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegisterSpeciesSelector(
    selected: EspecieAnimal?,
    enabled: Boolean,
    error: String?,
    onSelected: (EspecieAnimal) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
        Text(
            text = "Especie",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
        ) {
            registerSpeciesOptions.forEach { (option, label) ->
                RegisterSpeciesOption(
                    label = label,
                    imageRes = getAnimalImageRes(option),
                    selected = selected == option,
                    enabled = enabled,
                    onClick = { onSelected(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun RegisterSpeciesOption(
    label: String,
    imageRes: Int?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .heightIn(min = 72.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = label
            },
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GanAppSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs, Alignment.CenterVertically),
        ) {
            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(GanAppSpacing.xl),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Spacer(modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RegisterInformationCard(
    nombre: String,
    onNombreChange: (String) -> Unit,
    edad: String,
    onEdadChange: (String) -> Unit,
    peso: String,
    onPesoChange: (String) -> Unit,
    color: String,
    onColorChange: (String) -> Unit,
    enabled: Boolean,
    nameError: String?,
    ageError: String?,
    weightError: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(GanAppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Información",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            RegisterLabeledField(
                label = "Nombre del animal",
                value = nombre,
                onValueChange = onNombreChange,
                enabled = enabled,
                error = nameError,
                placeholder = "Ej: Manchita",
                leadingIcon = Icons.AutoMirrored.Outlined.Label,
            )
            RegisterLabeledField(
                label = "Edad (años)",
                value = edad,
                onValueChange = onEdadChange,
                enabled = enabled,
                error = ageError,
                keyboardType = KeyboardType.Number,
                suffix = "años",
                leadingIcon = Icons.Outlined.CalendarMonth,
            )
            RegisterLabeledField(
                label = "Peso (kg)",
                value = peso,
                onValueChange = onPesoChange,
                enabled = enabled,
                error = weightError,
                keyboardType = KeyboardType.Number,
                suffix = "kg",
                leadingIcon = Icons.Outlined.Scale,
            )
            RegisterLabeledField(
                label = "Color",
                value = color,
                onValueChange = onColorChange,
                enabled = enabled,
                error = null,
                leadingIcon = Icons.Outlined.Palette,
            )
        }
    }
}

@Composable
private fun RegisterLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    suffix: String? = null,
    leadingIcon: ImageVector? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            isError = error != null,
            singleLine = true,
            placeholder = placeholder?.let { text -> { Text(text) } },
            suffix = suffix?.let { text -> { Text(text) } },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                    )
                }
            },
            supportingText = error?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
        )
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
