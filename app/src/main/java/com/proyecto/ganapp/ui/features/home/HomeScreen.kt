package com.proyecto.ganapp.ui.features.home

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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

@Composable
fun HomeScreen(
    idUsuario: Long,
    onRegistrarAnimal: () -> Unit,
    onVerAnimales: () -> Unit,
    onNotificaciones: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val usuario by viewModel.usuario.collectAsState()
    val logoutUiState by viewModel.logoutUiState.collectAsState()

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

            Text(
                text = "Tus animales",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(GanAppSpacing.md))

            HomeDestinationRow(
                title = "Animales",
                subtitle = "Consulta tus animales registrados",
                icon = Icons.Outlined.Pets,
                onClick = onVerAnimales,
            )

            Spacer(modifier = Modifier.height(GanAppSpacing.md))

            HomeDestinationRow(
                title = "Notificaciones",
                subtitle = "Consulta y gestiona recordatorios",
                icon = Icons.Outlined.Notifications,
                onClick = onNotificaciones,
            )

            Spacer(modifier = Modifier.height(GanAppSpacing.xl))
        }
    }
}

@Composable
private fun HomeHeader(
    nombre: String?,
    logoutUiState: HomeLogoutUiState,
    onLogout: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Bienvenido",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!nombre.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(GanAppSpacing.xxs))
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
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
                tint = MaterialTheme.colorScheme.onSurface,
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
private fun HomeDestinationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GanAppSpacing.xxl),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(GanAppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
