package com.proyecto.ganapp.ui.features.animals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailScreen(
    animalId: Long,
    onBack: () -> Unit,
    onEditAnimal: (Long) -> Unit,
    viewModel: AnimalDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(animalId) {
        viewModel.loadAnimal(animalId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ficha del animal", textAlign = TextAlign.Center) },
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
                actions = {
                    if (uiState is AnimalDetailUiState.Success) {
                        IconButton(onClick = { onEditAnimal(animalId) }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar animal",
                            )
                        }
                    }
                },
                colors = ganAppTopAppBarColors(),
            )
        },
    ) { padding ->
        when (val state = uiState) {
            AnimalDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = "Cargando la ficha del animal"
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            AnimalDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(GanAppSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(GanAppSpacing.sm))
                        Text(
                            text = "Animal no encontrado",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(GanAppSpacing.xs))
                        Text(
                            text = "No fue posible encontrar el animal solicitado.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            is AnimalDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
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
                            modifier = Modifier.padding(GanAppSpacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.height(GanAppSpacing.sm))
                            Text(
                                text = "No fue posible cargar la información del animal.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            is AnimalDetailUiState.Success -> {
                AnimalDetailContent(
                    animal = state.animal,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun AnimalDetailContent(
    animal: Animal,
    modifier: Modifier = Modifier,
) {
    val rows = buildList {
        add(AnimalDetailRow("Nombre", animal.nombre))
        if (shouldShowBreed(animal.raza)) {
            add(AnimalDetailRow("Raza", animal.raza))
        }
        addAll(buildAnimalDetailRows(animal))
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(GanAppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
            Text(
                text = "Foto del animal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AnimalDetailPhoto(animal)
        }
        Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
            Text(
                text = "Especie",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AnimalSpeciesChip(especie = animal.especie)
        }
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
                    )
                }
                rows.forEach { row ->
                    Column(
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
                    ) {
                        Text(
                            text = row.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = row.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimalDetailPhoto(animal: Animal) {
    val uri = animal.fotoUri
    var photoFailed by remember(uri) { mutableStateOf(false) }
    val modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(4f / 3f)
        .clip(MaterialTheme.shapes.large)
    if (!uri.isNullOrBlank() && !photoFailed) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { photoFailed = true },
        )
    } else {
        AnimalPhotoPlaceholder(
            especie = animal.especie,
            modifier = modifier,
        )
    }
}

@Composable
private fun AnimalPhotoPlaceholder(
    especie: EspecieAnimal,
    modifier: Modifier = Modifier,
) {
    val imageRes = getAnimalImageRes(especie)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(GanAppSpacing.lg),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "Sin especificar",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
