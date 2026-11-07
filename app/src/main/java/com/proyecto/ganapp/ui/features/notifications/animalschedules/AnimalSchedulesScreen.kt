package com.proyecto.ganapp.ui.features.notifications.animalschedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle
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
    AnimalSchedulesScreen(state = state, onBack = { leaveAnimalSchedules(onBack) }, onRetry = viewModel::load)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalSchedulesScreen(
    state: AnimalSchedulesContent,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando programaciones" })
                }
                is AnimalSchedulesContent.Empty -> item { Text(AnimalSchedulesEmptyMessage) }
                is AnimalSchedulesContent.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry, modifier = Modifier.heightIn(min = GanAppSpacing.xxl)) {
                            Text("Reintentar")
                        }
                    }
                }
                is AnimalSchedulesContent.Success -> items(
                    state.schedules,
                    key = { it.programacion.idProgramacion },
                ) { detail ->
                    ScheduleDetailCard(detail)
                }
            }
        }
    }
}

@Composable
private fun AnimalSchedulesContextCard(animal: Animal) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(GanAppSpacing.md).semantics {
                contentDescription = "Animal ${animal.nombre}, ${animalSpeciesLabel(animal.especie)}"
            },
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
        ) {
            Text(animal.nombre, style = MaterialTheme.typography.titleMedium)
            Text(animalSpeciesLabel(animal.especie), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ScheduleDetailCard(detail: ProgramacionNotificacionDetalle) {
    val schedule = detail.programacion
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(GanAppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
        ) {
            Text(detail.plantilla.titulo, style = MaterialTheme.typography.titleMedium)
            Text(detail.plantilla.tipo.toPersistentString(), style = MaterialTheme.typography.bodyMedium)
            Text("Fecha inicio ${schedule.fechaInicio.format(DateFormat)}")
            Text("Fecha fin ${schedule.fechaFin.format(DateFormat)}")
            Text("Hora ${schedule.hora.format(TimeFormat)}")
            Text(schedule.recurrencia.toPersistentString())
        }
    }
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
