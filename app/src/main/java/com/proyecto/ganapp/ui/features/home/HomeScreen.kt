package com.proyecto.ganapp.ui.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing
import com.proyecto.ganapp.ui.features.animals.AnimalSpeciesChip
import com.proyecto.ganapp.ui.features.animals.buildAnimalCardMetadata
import com.proyecto.ganapp.ui.features.animals.getAnimalImageRes

@Composable
fun HomeScreen(
    idUsuario: Long,
    onRegistrarAnimal: () -> Unit,
    onVerAnimales: () -> Unit,
    onNotificaciones: () -> Unit,
    onAssignNotification: (Long) -> Unit,
    onViewNotifications: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val usuario by viewModel.usuario.collectAsState()
    val logoutUiState by viewModel.logoutUiState.collectAsState()
    val favoritesUiState by viewModel.favoritesUiState.collectAsState()

    LaunchedEffect(idUsuario) {
        viewModel.cargarUsuario(idUsuario)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = GanAppSpacing.lg,
                    vertical = GanAppSpacing.lg,
                ),
        ) {
            HomeHeader(
                nombre = usuario?.nombre,
                logoutUiState = logoutUiState,
                onLogout = viewModel::logout,
            )

            if (logoutUiState.error == HomeLogoutError.UNEXPECTED) {
                Spacer(modifier = Modifier.height(GanAppSpacing.md))
                HomeLogoutErrorBanner()
            }

            Spacer(modifier = Modifier.height(GanAppSpacing.xl))

            Button(
                onClick = onRegistrarAnimal,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(GanAppSpacing.xs))
                Text(
                    text = "Registrar animal",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(GanAppSpacing.xl))

            HomeFavoritesSection(
                state = favoritesUiState,
                onAssignNotification = onAssignNotification,
                onViewNotifications = onViewNotifications,
            )

            Spacer(modifier = Modifier.height(GanAppSpacing.xl))
        }
    }
}

@Composable
private fun HomeFavoritesSection(
    state: HomeFavoritesUiState,
    onAssignNotification: (Long) -> Unit,
    onViewNotifications: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.md)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Mis animales favoritos",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        when (state) {
            HomeFavoritesUiState.Loading -> HomeFavoritesLoading()
            is HomeFavoritesUiState.Success -> {
                if (state.animals.isEmpty()) {
                    HomeFavoritesEmpty()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                        state.animals.forEach { animal ->
                            HomeFavoriteCard(
                                animal = animal,
                                onAssignNotification = onAssignNotification,
                                onViewNotifications = onViewNotifications,
                            )
                        }
                    }
                }
            }
            is HomeFavoritesUiState.Error -> HomeFavoritesError()
        }
    }
}

@Composable
private fun HomeFavoritesLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GanAppSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(GanAppSpacing.xl),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun HomeFavoritesEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GanAppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(GanAppSpacing.xl),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Aún no tienes animales favoritos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Puedes marcar un animal como favorito desde Editar animal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeFavoritesError() {
    Text(
        text = "No fue posible cargar tus animales favoritos.",
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(GanAppSpacing.md),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Composable
private fun HomeFavoriteCard(
    animal: Animal,
    onAssignNotification: (Long) -> Unit,
    onViewNotifications: (Long) -> Unit,
) {
    val metadata = buildAnimalCardMetadata(animal)
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
            modifier = Modifier.padding(GanAppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            HomeFavoritePhoto(animal = animal)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
            ) {
                Text(
                    text = animal.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimalSpeciesChip(especie = animal.especie)
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
                FilledTonalButton(
                    onClick = { onAssignNotification(animal.idAnimal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GanAppSpacing.xxl),
                ) {
                    Text("Asignar")
                }
                OutlinedButton(
                    onClick = { onViewNotifications(animal.idAnimal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GanAppSpacing.xxl),
                ) {
                    Text("Notificaciones")
                }
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
    }
}

@Composable
private fun HomeFavoritePhoto(animal: Animal) {
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
        val imageRes = getAnimalImageRes(animal.especie)
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
                        modifier = Modifier.size(GanAppSpacing.xxl),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    nombre: String?,
    logoutUiState: HomeLogoutUiState,
    onLogout: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GanAppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bienvenido",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (!nombre.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(GanAppSpacing.xxs))
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.width(GanAppSpacing.sm))
            HomeAccountMenu(
                isLoading = logoutUiState.isLoading,
                hasError = logoutUiState.error == HomeLogoutError.UNEXPECTED,
                onLogout = onLogout,
            )
        }
    }
}

@Composable
private fun HomeAccountMenu(
    isLoading: Boolean,
    hasError: Boolean,
    onLogout: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(hasError) {
        if (hasError) expanded = false
    }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(GanAppSpacing.xxl),
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Cuenta",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                if (!isLoading) expanded = false
            },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (isLoading) "Cerrando sesión…" else "Cerrar sesión",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                leadingIcon = if (isLoading) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(GanAppSpacing.md),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
                enabled = !isLoading,
                onClick = onLogout,
            )
        }
    }
}

@Composable
private fun HomeLogoutErrorBanner() {
    val message = "No se pudo cerrar la sesión. Intenta de nuevo."
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .semantics(mergeDescendants = true) {
                error(message)
                liveRegion = LiveRegionMode.Polite
            }
            .padding(
                horizontal = GanAppSpacing.md,
                vertical = GanAppSpacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(GanAppSpacing.lg),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
