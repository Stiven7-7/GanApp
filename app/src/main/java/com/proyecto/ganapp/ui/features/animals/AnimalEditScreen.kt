package com.proyecto.ganapp.ui.features.animals

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalValidationError
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

private val editSpeciesOptions = listOf(
    EspecieAnimal.VACA to "Vaca",
    EspecieAnimal.CERDO to "Cerdo",
    EspecieAnimal.OVEJA to "Oveja",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalEditScreen(
    animalId: Long,
    onBack: () -> Unit,
    onUpdateSuccess: () -> Unit,
    viewModel: AnimalEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(animalId) {
        viewModel.loadAnimal(animalId)
    }

    LaunchedEffect(uiState) {
        if (uiState is AnimalEditUiState.Success) {
            onUpdateSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar animal", textAlign = TextAlign.Center) },
                expandedHeight = if (LocalDensity.current.fontScale >= 1.5f) {
                    GanAppSpacing.xxl * 3
                } else {
                    TopAppBarDefaults.TopAppBarExpandedHeight
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        val state = uiState
        val editingAnimal = state.editingAnimal()
        when {
            state is AnimalEditUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = "Cargando la edición del animal"
                        },
                    )
                }
            }

            state is AnimalEditUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(GanAppSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Animal no encontrado",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            state is AnimalEditUiState.Error && state.animal == null -> {
                LoadError(modifier = Modifier.padding(padding))
            }

            editingAnimal != null -> {
                AnimalEditForm(
                    animal = editingAnimal,
                    saving = state is AnimalEditUiState.Saving || state is AnimalEditUiState.Success,
                    errors = (state as? AnimalEditUiState.ValidationError)?.errors.orEmpty(),
                    saveMessage = if (state is AnimalEditUiState.Error) {
                        "No fue posible guardar los cambios."
                    } else {
                        null
                    },
                    onSave = viewModel::saveAnimal,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun LoadError(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(GanAppSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Column(
                modifier = Modifier.padding(GanAppSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(GanAppSpacing.xl),
                )
                Spacer(modifier = Modifier.size(GanAppSpacing.sm))
                Text(
                    text = "No fue posible cargar la información del animal.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AnimalEditForm(
    animal: Animal,
    saving: Boolean,
    errors: Set<UpdateAnimalValidationError>,
    saveMessage: String?,
    onSave: (
        especie: EspecieAnimal?,
        nombre: String,
        edad: Int?,
        peso: Float?,
        color: String?,
        fotoUri: String?,
        esFavorito: Boolean,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var especie by remember(animal.idAnimal) {
        mutableStateOf(initialEditableSpecies(animal.especie))
    }
    var nombre by remember(animal.idAnimal) { mutableStateOf(animal.nombre) }
    var edad by remember(animal.idAnimal) { mutableStateOf(formatEditableAge(animal.edad)) }
    var peso by remember(animal.idAnimal) { mutableStateOf(formatEditableWeight(animal.peso)) }
    var color by remember(animal.idAnimal) { mutableStateOf(animal.color.orEmpty()) }
    var fotoUri by remember(animal.idAnimal) { mutableStateOf(animal.fotoUri) }
    var esFavorito by remember(animal.idAnimal) { mutableStateOf(animal.esFavorito) }
    val originalEspecie = remember(animal.idAnimal) { animal.especie }
    var localEdadError by remember(animal.idAnimal) { mutableStateOf<String?>(null) }
    var localPesoError by remember(animal.idAnimal) { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) fotoUri = uri.toString()
    }

    val speciesError = errors
        .filter {
            it == UpdateAnimalValidationError.SPECIES_REQUIRED ||
                it == UpdateAnimalValidationError.SPECIES_NOT_ALLOWED
        }
        .sortedBy { it.ordinal }
        .joinToString(separator = "\n") { updateValidationMessage(it) }
        .ifBlank { null }
    val nameError = errors
        .firstOrNull { it == UpdateAnimalValidationError.NAME_REQUIRED }
        ?.let { updateValidationMessage(it) }
    val ageError = localEdadError ?: errors
        .firstOrNull { it == UpdateAnimalValidationError.INVALID_AGE }
        ?.let { updateValidationMessage(it) }
    val weightError = localPesoError ?: errors
        .firstOrNull { it == UpdateAnimalValidationError.INVALID_WEIGHT }
        ?.let { updateValidationMessage(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(GanAppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
    ) {
        EditAnimalPhoto(
            fotoUri = fotoUri,
            fallbackSpecies = especie ?: animal.especie,
            enabled = !saving,
            onPickPhoto = { launcher.launch("image/*") },
            onRemovePhoto = { fotoUri = null },
        )
        EditSpeciesSelector(
            selected = especie,
            enabled = !saving,
            error = null,
            onSelected = { if (!saving) especie = it },
        )
        if (originalEspecie == EspecieAnimal.SIN_ESPECIFICAR && especie == null) {
            Text(
                text = "Selecciona una especie para continuar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (speciesError != null) {
            Text(
                text = speciesError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        EditInformationCard(
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
            enabled = !saving,
            nameError = nameError,
            ageError = ageError,
            weightError = weightError,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(GanAppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Favorito",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = esFavorito,
                    onCheckedChange = { esFavorito = it },
                    enabled = !saving,
                    modifier = Modifier.semantics { contentDescription = "Favorito" },
                )
            }
        }
        if (saveMessage != null) {
            Text(
                text = saveMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                if (saving) return@Button
                val parsedEdad = parseOptionalInt(edad)
                val parsedPeso = parseOptionalFloat(peso)
                val edadInvalida = edad.isNotBlank() && parsedEdad == null
                val pesoInvalido = peso.isNotBlank() && parsedPeso == null
                localEdadError = if (edadInvalida) "La edad no es válida." else null
                localPesoError = if (pesoInvalido) "El peso no es válido." else null
                if (edadInvalida || pesoInvalido) return@Button
                onSave(especie, nombre, parsedEdad, parsedPeso, color, fotoUri, esFavorito)
            },
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.size(GanAppSpacing.xs))
                Text("Guardando…")
            } else {
                Icon(imageVector = Icons.Outlined.Save, contentDescription = null)
                Spacer(modifier = Modifier.size(GanAppSpacing.xs))
                Text(
                    text = "Guardar cambios",
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditAnimalPhoto(
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
                        Text(
                            text = "Sin especificar",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(GanAppSpacing.md),
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
private fun EditSpeciesSelector(
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
            editSpeciesOptions.forEach { (option, label) ->
                EditSpeciesOption(
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
private fun EditSpeciesOption(
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
private fun EditInformationCard(
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
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            EditLabeledField(
                label = "Nombre del animal",
                value = nombre,
                onValueChange = onNombreChange,
                enabled = enabled,
                error = nameError,
                placeholder = "Ej: Manchita",
                leadingIcon = Icons.AutoMirrored.Outlined.Label,
            )
            EditLabeledField(
                label = "Edad (años)",
                value = edad,
                onValueChange = onEdadChange,
                enabled = enabled,
                error = ageError,
                keyboardType = KeyboardType.Number,
                suffix = "años",
                leadingIcon = Icons.Outlined.CalendarMonth,
            )
            EditLabeledField(
                label = "Peso (kg)",
                value = peso,
                onValueChange = onPesoChange,
                enabled = enabled,
                error = weightError,
                keyboardType = KeyboardType.Decimal,
                suffix = "kg",
                leadingIcon = Icons.Outlined.Scale,
            )
            EditLabeledField(
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
private fun EditLabeledField(
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


private fun AnimalEditUiState.editingAnimal(): Animal? {
    return when (this) {
        is AnimalEditUiState.Ready -> animal
        is AnimalEditUiState.Saving -> animal
        is AnimalEditUiState.ValidationError -> animal
        is AnimalEditUiState.Success -> animal
        is AnimalEditUiState.Error -> animal
        AnimalEditUiState.Loading,
        AnimalEditUiState.NotFound,
        -> null
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
