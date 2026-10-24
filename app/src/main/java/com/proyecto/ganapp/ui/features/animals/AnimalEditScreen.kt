package com.proyecto.ganapp.ui.features.animals

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.UpdateAnimalValidationError

private val editableSpecies = listOf(
    EspecieAnimal.VACA to "Vaca",
    EspecieAnimal.CERDO to "Cerdo",
    EspecieAnimal.OVEJA to "Oveja",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar animal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
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
                        .padding(32.dp),
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
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No fue posible cargar la información del animal.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditPhoto(
            especie = especie ?: animal.especie,
            fotoUri = fotoUri,
            enabled = !saving,
            onChangePhoto = { launcher.launch("image/*") },
            onRemovePhoto = { fotoUri = null },
        )
        Text(
            text = "Especie",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            editableSpecies.forEach { (option, label) ->
                FilterChip(
                    selected = especie == option,
                    onClick = { if (!saving) especie = option },
                    enabled = !saving,
                    label = { Text(label) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
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
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            enabled = !saving,
            isError = nameError != null,
            supportingText = nameError?.let { message -> { Text(message) } },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = edad,
            onValueChange = {
                edad = it
                localEdadError = null
            },
            label = { Text("Edad") },
            suffix = { Text("años") },
            enabled = !saving,
            isError = ageError != null,
            supportingText = ageError?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = peso,
            onValueChange = {
                peso = it
                localPesoError = null
            },
            label = { Text("Peso") },
            suffix = { Text("kg") },
            enabled = !saving,
            isError = weightError != null,
            supportingText = weightError?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            label = { Text("Color") },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        )
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
                onSave(especie, nombre, parsedEdad, parsedPeso, color, fotoUri)
            },
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Guardando…")
            } else {
                Text("Guardar cambios")
            }
        }
    }
}

@Composable
private fun EditPhoto(
    especie: EspecieAnimal,
    fotoUri: String?,
    enabled: Boolean,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    var photoFailed by remember(fotoUri) { mutableStateOf(false) }
    val modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(4f / 3f)
        .clip(MaterialTheme.shapes.large)
    if (!fotoUri.isNullOrBlank() && !photoFailed) {
        AsyncImage(
            model = fotoUri,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { photoFailed = true },
        )
    } else {
        val imageRes = getAnimalImageRes(especie)
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (imageRes != null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "Sin especificar",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onChangePhoto,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
        ) {
            Text("Cambiar foto")
        }
        if (fotoUri != null) {
            TextButton(
                onClick = onRemovePhoto,
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Quitar foto")
            }
        }
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
