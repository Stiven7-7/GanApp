package com.proyecto.ganapp.ui.features.animals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.proyecto.ganapp.R
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.Favorite
import com.proyecto.ganapp.ui.common.theme.FavoriteContainer
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalsScreen(
    userId: Long,
    onAddAnimal: () -> Unit,
    onAssignNotification: (Long) -> Unit,
    onViewNotifications: (Long) -> Unit,
    onAnimalClick: (Long) -> Unit,
    viewModel: AnimalsViewModel = hiltViewModel()
) {
    val listState by viewModel.listUiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val speciesFilter by viewModel.speciesFilter.collectAsState()
    val speciesCounts by viewModel.speciesCounts.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadAnimals(userId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Animales") },
                colors = ganAppTopAppBarColors(),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Agregar animal") },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                onClick = onAddAnimal,
                modifier = Modifier.semantics {
                    contentDescription = "Agregar animal"
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    ) { padding ->
        when (val state = listState) {
            AnimalsListUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            AnimalsListUiState.Empty -> {
                AnimalsEmptyState(
                    onAddAnimal = onAddAnimal,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                )
            }

            is AnimalsListUiState.Error -> {
                AnimalsErrorState(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                )
            }

            AnimalsListUiState.NoResults,
            is AnimalsListUiState.Success,
            -> {
                AnimalsDiscoveryList(
                    animals = (state as? AnimalsListUiState.Success)?.animals,
                    onAnimalClick = onAnimalClick,
                    onAssignNotification = onAssignNotification,
                    onViewNotifications = onViewNotifications,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    header = {
                        AnimalsSearchAndFilters(
                            searchQuery = searchQuery,
                            speciesFilter = speciesFilter,
                            speciesCounts = speciesCounts,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSpeciesFilterChange = viewModel::updateSpeciesFilter,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimalsSearchAndFilters(
    searchQuery: String,
    speciesFilter: AnimalSpeciesFilter,
    speciesCounts: AnimalSpeciesCounts,
    onSearchQueryChange: (String) -> Unit,
    onSpeciesFilterChange: (AnimalSpeciesFilter) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar por nombre") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Limpiar búsqueda",
                        )
                    }
                }
            } else {
                null
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            AnimalSpeciesFilterChip(
                label = "Todos",
                count = speciesCounts.total,
                selected = speciesFilter == AnimalSpeciesFilter.ALL,
                onClick = { onSpeciesFilterChange(AnimalSpeciesFilter.ALL) },
            )
            AnimalSpeciesFilterChip(
                label = "Vacas",
                count = speciesCounts.vacas,
                selected = speciesFilter == AnimalSpeciesFilter.VACA,
                onClick = { onSpeciesFilterChange(AnimalSpeciesFilter.VACA) },
            )
            AnimalSpeciesFilterChip(
                label = "Cerdos",
                count = speciesCounts.cerdos,
                selected = speciesFilter == AnimalSpeciesFilter.CERDO,
                onClick = { onSpeciesFilterChange(AnimalSpeciesFilter.CERDO) },
            )
            AnimalSpeciesFilterChip(
                label = "Ovejas",
                count = speciesCounts.ovejas,
                selected = speciesFilter == AnimalSpeciesFilter.OVEJA,
                onClick = { onSpeciesFilterChange(AnimalSpeciesFilter.OVEJA) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimalSpeciesFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label)
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {},
    )
}

@Composable
private fun AnimalsDiscoveryList(
    animals: List<Animal>?,
    onAnimalClick: (Long) -> Unit,
    onAssignNotification: (Long) -> Unit,
    onViewNotifications: (Long) -> Unit,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fabClearance = 56.dp + GanAppSpacing.md + GanAppSpacing.sm
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = GanAppSpacing.md,
            top = GanAppSpacing.md,
            end = GanAppSpacing.md,
            bottom = fabClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
    ) {
        item { header() }
        if (animals != null) {
            items(
                items = animals,
                key = { animal -> animal.idAnimal },
            ) { animal ->
                AnimalCard(
                    animal = animal,
                    onAnimalClick = onAnimalClick,
                    onAssignNotification = onAssignNotification,
                    onViewNotifications = onViewNotifications,
                )
            }
        } else {
            item { AnimalsNoResultsState() }
        }
    }
}

@Composable
private fun AnimalsNoResultsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(GanAppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(GanAppSpacing.xl),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(GanAppSpacing.sm))
        Text(
            text = "No se encontraron animales.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(GanAppSpacing.xs))
        Text(
            text = "Prueba con otra búsqueda o especie.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AnimalCard(
    animal: Animal,
    onAnimalClick: (Long) -> Unit,
    onAssignNotification: (Long) -> Unit,
    onViewNotifications: (Long) -> Unit
) {
    val metadata = buildAnimalCardMetadata(animal)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        BoxWithConstraints {
            val stackActions = maxWidth < 320.dp || LocalDensity.current.fontScale >= 1.5f
            Column(modifier = Modifier.padding(GanAppSpacing.md)) {
                if (stackActions) {
                    AnimalCardIdentityCompact(
                        animal = animal,
                        metadata = metadata,
                        onAnimalClick = onAnimalClick,
                    )
                } else {
                    AnimalCardIdentity(
                        animal = animal,
                        metadata = metadata,
                        onAnimalClick = onAnimalClick,
                    )
                }
                Spacer(modifier = Modifier.height(GanAppSpacing.md))
                if (stackActions) {
                    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                        FilledTonalButton(
                            onClick = { onAssignNotification(animal.idAnimal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = GanAppSpacing.xxl),
                            contentPadding = PaddingValues(
                                horizontal = GanAppSpacing.md,
                                vertical = GanAppSpacing.xs,
                            ),
                        ) {
                            Text("Asignar")
                        }
                        OutlinedButton(
                            onClick = { onViewNotifications(animal.idAnimal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = GanAppSpacing.xxl),
                            contentPadding = PaddingValues(
                                horizontal = GanAppSpacing.md,
                                vertical = GanAppSpacing.xs,
                            ),
                        ) {
                            Text(
                                text = "Ver Notificaciones",
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                        FilledTonalButton(
                            onClick = { onAssignNotification(animal.idAnimal) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = GanAppSpacing.xxl),
                        ) {
                            Text("Asignar")
                        }
                        OutlinedButton(
                            onClick = { onViewNotifications(animal.idAnimal) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = GanAppSpacing.xxl),
                        ) {
                            Text("Ver Notificaciones")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimalCardIdentity(
    animal: Animal,
    metadata: AnimalCardMetadata?,
    onAnimalClick: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GanAppSpacing.xxl)
            .semantics(mergeDescendants = true) {}
            .clickable(
                onClickLabel = "Ver ficha de ${animal.nombre}",
                onClick = { onAnimalClick(animal.idAnimal) },
            ),
        horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        AnimalListPhoto(animal = animal)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
        ) {
            AnimalCardName(animal.nombre)
            AnimalSpeciesChip(especie = animal.especie)
            AnimalCardMetadataLines(metadata)
        }
        if (animal.esFavorito) {
            AnimalFavoriteIndicator()
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnimalCardIdentityCompact(
    animal: Animal,
    metadata: AnimalCardMetadata?,
    onAnimalClick: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GanAppSpacing.xxl)
            .semantics(mergeDescendants = true) {}
            .clickable(
                onClickLabel = "Ver ficha de ${animal.nombre}",
                onClick = { onAnimalClick(animal.idAnimal) },
            ),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimalListPhoto(animal = animal)
            AnimalCardName(
                nombre = animal.nombre,
                modifier = Modifier.weight(1f),
            )
            if (animal.esFavorito) {
                AnimalFavoriteIndicator()
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimalSpeciesChip(especie = animal.especie)
        AnimalCardMetadataLines(metadata)
    }
}

@Composable
private fun AnimalFavoriteIndicator() {
    Surface(
        shape = CircleShape,
        color = FavoriteContainer,
        contentColor = Favorite,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Favorito",
            modifier = Modifier
                .padding(GanAppSpacing.xxs)
                .size(GanAppSpacing.md),
        )
    }
}

@Composable
private fun AnimalCardName(
    nombre: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = nombre,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AnimalCardMetadataLines(metadata: AnimalCardMetadata?) {
    if (metadata?.lineOne != null) {
        Text(
            text = metadata.lineOne,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (metadata?.lineTwo != null) {
        Text(
            text = metadata.lineTwo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnimalListPhoto(animal: Animal) {
    val uri = animal.fotoUri
    var photoFailed by remember(uri) { mutableStateOf(false) }
    val modifier = Modifier
        .size(80.dp)
        .clip(MaterialTheme.shapes.medium)
    if (!uri.isNullOrBlank() && !photoFailed) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { photoFailed = true },
        )
    } else {
        AnimalListPhotoFallback(
            especie = animal.especie,
            modifier = modifier,
        )
    }
}

@Composable
private fun AnimalListPhotoFallback(
    especie: EspecieAnimal,
    modifier: Modifier = Modifier,
) {
    val imageRes = getAnimalImageRes(especie)
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun AnimalsEmptyState(
    onAddAnimal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(GanAppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(160.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.def_vaca),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Spacer(modifier = Modifier.height(GanAppSpacing.lg))
        Text(
            text = "No tienes animales registrados",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(GanAppSpacing.xs))
        Text(
            text = "Registra tu primer animal para comenzar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(GanAppSpacing.lg))
        Button(
            onClick = onAddAnimal,
            modifier = Modifier.heightIn(min = GanAppSpacing.xxl),
        ) {
            Text("Registrar animal")
        }
    }
}

@Composable
private fun AnimalsErrorState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(GanAppSpacing.xl),
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
                    text = "No fue posible cargar los animales.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
