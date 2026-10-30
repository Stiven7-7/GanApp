package com.proyecto.ganapp.ui.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.ui.common.theme.Favorite
import com.proyecto.ganapp.ui.common.theme.FavoriteContainer
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing
import com.proyecto.ganapp.ui.common.theme.OnFavoriteContainer
import com.proyecto.ganapp.ui.features.animals.AnimalSpeciesChip
import com.proyecto.ganapp.ui.features.animals.getAnimalImageRes
import com.proyecto.ganapp.ui.features.animals.shouldShowBreed

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

            HomeRegisterAnimalAction(onClick = onRegistrarAnimal)

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
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            Surface(
                modifier = Modifier.size(GanAppSpacing.xl),
                shape = MaterialTheme.shapes.small,
                color = FavoriteContainer,
                contentColor = OnFavoriteContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Favorite,
                    )
                }
            }
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
            tint = Favorite,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(GanAppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    HomeFavoritePhoto(animal = animal)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
                ) {
                    HomeFavoriteIdentity(animal = animal)
                    HomeFavoriteMetadata(animal = animal)
                }
                HomeFavoriteMark()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            HomeFavoriteActions(
                onAssign = { onAssignNotification(animal.idAnimal) },
                onViewNotifications = { onViewNotifications(animal.idAnimal) },
            )
        }
    }
}

@Composable
private fun HomeFavoriteMark() {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = FavoriteContainer,
        contentColor = OnFavoriteContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Favorito",
                modifier = Modifier.size(20.dp),
                tint = Favorite,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeFavoriteIdentity(animal: Animal) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
    ) {
        Text(
            text = animal.nombre,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AnimalSpeciesChip(especie = animal.especie)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeFavoriteMetadata(animal: Animal) {
    val chips = buildList {
        animal.edad?.let { edad -> add(formatHomeFavoriteAge(edad)) }
        animal.peso?.let { peso -> add("$peso kg") }
        animal.color?.takeIf { it.isNotBlank() }?.let { color -> add("Color $color") }
        if (shouldShowBreed(animal.raza)) {
            add("Raza ${animal.raza}")
        }
    }
    if (chips.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
    ) {
        chips.forEach { label ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(
                        horizontal = GanAppSpacing.xs,
                        vertical = GanAppSpacing.xxs,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun HomeFavoriteActions(
    onAssign: () -> Unit,
    onViewNotifications: () -> Unit,
) {
    val actionPadding = PaddingValues(
        horizontal = GanAppSpacing.md,
        vertical = GanAppSpacing.xs,
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GanAppSpacing.md),
    ) {
        val stackActions = maxWidth < 320.dp || LocalDensity.current.fontScale >= 1.5f
        if (stackActions) {
            Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                HomeAssignAction(
                    onClick = onAssign,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = actionPadding,
                )
                HomeNotificationsAction(
                    onClick = onViewNotifications,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = actionPadding,
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                HomeAssignAction(
                    onClick = onAssign,
                    modifier = Modifier.weight(1f),
                    contentPadding = actionPadding,
                )
                HomeNotificationsAction(
                    onClick = onViewNotifications,
                    modifier = Modifier.weight(1f),
                    contentPadding = actionPadding,
                )
            }
        }
    }
}

@Composable
private fun HomeAssignAction(
    onClick: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = GanAppSpacing.xxl),
        contentPadding = contentPadding,
    ) {
        Text(text = "Asignar")
    }
}

@Composable
private fun HomeNotificationsAction(
    onClick: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = GanAppSpacing.xxl),
        contentPadding = contentPadding,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Text(text = "Notificaciones")
    }
}

@Composable
private fun HomeFavoritePhoto(animal: Animal) {
    val uri = animal.fotoUri
    var photoFailed by remember(uri) { mutableStateOf(false) }
    val modifier = Modifier
        .size(72.dp)
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
    val saludo = if (!nombre.isNullOrBlank()) "¡Hola, $nombre!" else "¡Hola!"
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
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            Surface(
                modifier = Modifier.size(GanAppSpacing.xxl),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(GanAppSpacing.xl),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = saludo,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Listo para trabajar en el campo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            HomeAccountMenu(
                isLoading = logoutUiState.isLoading,
                hasError = logoutUiState.error == HomeLogoutError.UNEXPECTED,
                onLogout = onLogout,
            )
        }
    }
}

@Composable
private fun HomeRegisterAnimalAction(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = GanAppSpacing.md,
                vertical = GanAppSpacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                    )
                }
            }
            Text(
                text = "Registrar nuevo animal",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
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
        Surface(
            modifier = Modifier.size(GanAppSpacing.xxl),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(GanAppSpacing.xxl),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Cuenta",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
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

private fun formatHomeFavoriteAge(edad: Int): String {
    return if (edad == 1) "1 año" else "$edad años"
}
