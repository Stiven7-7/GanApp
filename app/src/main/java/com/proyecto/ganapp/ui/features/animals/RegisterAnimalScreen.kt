package com.proyecto.ganapp.ui.features.animals

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.usecase.animal.RegisterAnimalValidationError

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
    var localFeedback by remember { mutableStateOf<String?>(null) }

    val registrationState by viewModel.registrationUiState.collectAsState()
    val isLoading = registrationState is AnimalRegistrationUiState.Loading
    val inputsEnabled = !isLoading && registrationState !is AnimalRegistrationUiState.Success

    LaunchedEffect(registrationState) {
        if (registrationState is AnimalRegistrationUiState.Success) {
            onAnimalSaved()
            viewModel.consumeRegistrationSuccess()
        }
    }

    // Launcher para seleccionar imagen (galería)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fotoUri = uri?.toString()
    }

    val feedback = localFeedback ?: registrationFeedback(registrationState)

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Registrar Animal") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            EspecieSelector(
                selected = especie,
                enabled = inputsEnabled,
                onSelected = { especie = it },
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del animal") },
                placeholder = { Text("Ej: Manchita") },
                enabled = inputsEnabled,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = edad,
                onValueChange = { edad = it },
                label = { Text("Edad (años)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = inputsEnabled,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = peso,
                onValueChange = { peso = it },
                label = { Text("Peso (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = inputsEnabled,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Color") },
                enabled = inputsEnabled,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Preview de la imagen seleccionada
            if (fotoUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = fotoUri),
                    contentDescription = "Preview foto",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { launcher.launch("image/*") },
                    enabled = inputsEnabled,
                ) {
                    Text("Seleccionar foto")
                }

                TextButton(
                    onClick = { fotoUri = null },
                    enabled = inputsEnabled,
                ) {
                    Text("Quitar foto")
                }
            }

            Spacer(Modifier.height(24.dp))

            if (feedback != null) {
                Text(
                    text = feedback,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (!inputsEnabled) return@Button
                    val parsedEdad = parseOptionalInt(edad)
                    val parsedPeso = parseOptionalFloat(peso)
                    val inputErrors = buildList {
                        if (edad.isNotBlank() && parsedEdad == null) {
                            add("La edad no es válida.")
                        }
                        if (peso.isNotBlank() && parsedPeso == null) {
                            add("El peso no es válido.")
                        }
                    }
                    if (inputErrors.isNotEmpty()) {
                        localFeedback = inputErrors.joinToString(separator = "\n")
                        return@Button
                    }
                    localFeedback = null
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
            ) {
                Text("Guardar en dispositivo")
            }
        }
    }
}

@Composable
private fun EspecieSelector(
    selected: EspecieAnimal?,
    enabled: Boolean,
    onSelected: (EspecieAnimal) -> Unit,
) {
    Text("Especie")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        selectableSpecies.forEach { (especie, label) ->
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == especie,
                    onClick = { onSelected(especie) },
                    enabled = enabled,
                )
                Text(label)
            }
        }
    }
}

private val selectableSpecies = listOf(
    EspecieAnimal.VACA to "Vaca",
    EspecieAnimal.CERDO to "Cerdo",
    EspecieAnimal.OVEJA to "Oveja",
)

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

private fun registrationFeedback(state: AnimalRegistrationUiState): String? {
    return when (state) {
        is AnimalRegistrationUiState.ValidationError ->
            state.errors
                .sortedBy { it.ordinal }
                .joinToString(separator = "\n") { error -> validationMessage(error) }
        is AnimalRegistrationUiState.Failure ->
            "No fue posible registrar el animal. Intenta nuevamente."
        AnimalRegistrationUiState.Idle,
        AnimalRegistrationUiState.Loading,
        is AnimalRegistrationUiState.Success,
        -> null
    }
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
