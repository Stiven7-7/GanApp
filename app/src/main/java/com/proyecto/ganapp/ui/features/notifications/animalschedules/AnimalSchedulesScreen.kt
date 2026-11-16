package com.proyecto.ganapp.ui.features.notifications.animalschedules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing
import com.proyecto.ganapp.ui.features.animals.animalSpeciesLabel
import java.time.format.DateTimeFormatter

/** Entrada aislada. Requiere ViewModel explícito; no entra al NavGraph productivo. */
@Composable
fun AnimalSchedulesRoute(
    viewModel: AnimalSchedulesViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val deletion by viewModel.deletion.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    AnimalSchedulesScreen(
        state = state,
        deletion = deletion,
        snackbar = snackbar,
        onBack = { leaveAnimalSchedules(onBack) },
        onRetry = viewModel::load,
        onDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalSchedulesScreen(
    state: AnimalSchedulesContent,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    deletion: AnimalScheduleDeletionUi = AnimalScheduleDeletionUi(),
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    onDelete: (ProgramacionNotificacionDetalle) -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissDelete: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Programaciones del animal", textAlign = TextAlign.Center) },
                expandedHeight = if (LocalDensity.current.fontScale >= 1.5f) {
                    GanAppSpacing.xxl * 3
                } else {
                    TopAppBarDefaults.TopAppBarExpandedHeight
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = ganAppTopAppBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(GanAppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            val animal = when (state) {
                is AnimalSchedulesContent.Empty -> state.animal
                is AnimalSchedulesContent.Success -> state.animal
                else -> null
            }
            if (animal != null) item { AnimalSchedulesContextCard(animal) }
            when (state) {
                AnimalSchedulesContent.Loading -> item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = GanAppSpacing.xl),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            Modifier.semantics { contentDescription = "Cargando programaciones" },
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                is AnimalSchedulesContent.Empty -> item {
                    Text(
                        AnimalSchedulesEmptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is AnimalSchedulesContent.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(onClick = onRetry, modifier = Modifier.heightIn(min = GanAppSpacing.xxl)) {
                            Text("Reintentar")
                        }
                    }
                }
                is AnimalSchedulesContent.Success -> items(
                    state.schedules,
                    key = { it.programacion.idProgramacion },
                ) { detail ->
                    ScheduleDetailCard(
                        detail = detail,
                        enabled = !deletion.isDeleting,
                        onDelete = { onDelete(detail) },
                    )
                }
            }
        }
    }
    deletion.target?.let { detail ->
        AlertDialog(
            onDismissRequest = { if (!deletion.isDeleting) onDismissDelete() },
            title = { Text("Eliminar programación") },
            text = { Text("¿Quieres eliminar la programación de ${detail.plantilla.titulo}?") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete, enabled = !deletion.isDeleting) {
                    Text(if (deletion.isDeleting) "Eliminando…" else "Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete, enabled = !deletion.isDeleting) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun AnimalSchedulesContextCard(animal: Animal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.padding(GanAppSpacing.md).semantics {
                contentDescription = "Animal ${animal.nombre}, ${animalSpeciesLabel(animal.especie)}"
            },
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
        ) {
            Text(animal.nombre, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                animalSpeciesLabel(animal.especie),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScheduleDetailCard(
    detail: ProgramacionNotificacionDetalle,
    enabled: Boolean,
    onDelete: () -> Unit,
) {
    val schedule = detail.programacion
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.padding(GanAppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScheduleTypeBadge(detail.plantilla.tipo)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs)) {
                    Text(
                        detail.plantilla.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ScheduleTypeLabel(detail.plantilla.tipo.toPersistentString())
                }
            }
            ScheduleFact(Icons.Outlined.CalendarMonth, "Fecha inicio", schedule.fechaInicio.format(DateFormat))
            ScheduleFact(Icons.Outlined.CalendarMonth, "Fecha fin", schedule.fechaFin.format(DateFormat))
            ScheduleFact(Icons.Outlined.Schedule, "Hora", schedule.hora.format(TimeFormat))
            ScheduleFact(Icons.Outlined.Autorenew, "Recurrencia", schedule.recurrencia.toPersistentString())
            TextButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "Eliminar programación: ${detail.plantilla.titulo}"
                    },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(GanAppSpacing.xs))
                Text("Eliminar")
            }
        }
    }
}

@Composable
private fun ScheduleFact(icon: ImageVector, label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ScheduleTypeBadge(type: TipoNotificacion) {
    Surface(
        modifier = Modifier.size(GanAppSpacing.xxl),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(type.scheduleListIcon(), contentDescription = null)
        }
    }
}

@Composable
private fun ScheduleTypeLabel(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = GanAppSpacing.sm, vertical = GanAppSpacing.xxs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun TipoNotificacion.scheduleListIcon(): ImageVector = when (this) {
    TipoNotificacion.VACUNACION -> Icons.Outlined.Vaccines
    TipoNotificacion.ALIMENTACION -> Icons.Outlined.Grass
    TipoNotificacion.CICLO_REPRODUCTIVO -> Icons.Outlined.Autorenew
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
