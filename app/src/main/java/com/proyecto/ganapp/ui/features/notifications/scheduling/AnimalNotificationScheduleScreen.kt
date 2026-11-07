package com.proyecto.ganapp.ui.features.notifications.scheduling

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing
import com.proyecto.ganapp.ui.features.animals.animalSpeciesLabel
import com.proyecto.ganapp.ui.features.notifications.NotificationFormSectionTitle
import com.proyecto.ganapp.ui.features.notifications.NotificationScheduleField
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Entrada aislada. Requiere ViewModel explícito; no entra al NavGraph productivo. */
@Composable
fun AnimalNotificationScheduleRoute(
    viewModel: AnimalNotificationScheduleViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AnimalScheduleEvent.Message -> snackbar.showSnackbar(event.text)
                is AnimalScheduleEvent.SchedulingFailed -> snackbar.showSnackbar(
                    "La programación ${event.scheduleId} se guardó, pero no se pudo activar la alerta.",
                )
                AnimalScheduleEvent.Completed -> onFinished()
            }
        }
    }
    BackHandler(enabled = state.step == AnimalScheduleStep.Configure || state.isSaving) {
        viewModel.backToSelection()
    }
    AnimalNotificationScheduleScreen(
        state = state,
        snackbar = snackbar,
        onBack = { if (!viewModel.backToSelection()) onBack() },
        onRetry = viewModel::load,
        onSelect = viewModel::selectTemplate,
        onContinue = viewModel::continueToConfigure,
        onStartDate = viewModel::changeStart,
        onEndDate = viewModel::changeEnd,
        onTime = viewModel::changeTime,
        onRecurrence = viewModel::changeRecurrence,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimalNotificationScheduleScreen(
    state: AnimalNotificationScheduleUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (Long) -> Unit,
    onContinue: () -> Unit,
    onStartDate: (LocalDate) -> Unit,
    onEndDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
    onRecurrence: (RecurrenciaNotificacion) -> Unit,
    onSave: () -> Unit,
) {
    val configuring = state.step == AnimalScheduleStep.Configure
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (configuring) "Configurar programación" else "Seleccionar plantilla",
                        textAlign = TextAlign.Center,
                    )
                },
                expandedHeight = if (LocalDensity.current.fontScale >= 1.5f) {
                    GanAppSpacing.xxl * 3
                } else {
                    TopAppBarDefaults.TopAppBarExpandedHeight
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = ganAppTopAppBarColors(),
            )
        },
        bottomBar = {
            val label = if (configuring) "Guardar programación" else "Continuar"
            Button(
                onClick = if (configuring) onSave else onContinue,
                enabled = if (configuring) !state.isSaving && state.selectedTemplate != null else state.canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GanAppSpacing.md)
                    .heightIn(min = GanAppSpacing.xxl),
            ) {
                Text(text = label, textAlign = TextAlign.Center)
            }
        },
    ) { padding ->
        if (configuring) {
            ConfigureScheduleContent(state, padding, onStartDate, onEndDate, onTime, onRecurrence)
        } else {
            SelectTemplateContent(state, padding, onRetry, onSelect)
        }
    }
}

@Composable
private fun SelectTemplateContent(
    state: AnimalNotificationScheduleUiState,
    padding: PaddingValues,
    onRetry: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize().imePadding(),
        contentPadding = PaddingValues(GanAppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
    ) {
        state.animal?.let { animal -> item { AnimalContextCard(animal) } }
        when (val load = state.load) {
            AnimalScheduleLoad.Loading -> item {
                CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando plantillas" })
            }
            is AnimalScheduleLoad.Error -> item {
                Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                    Text(load.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, modifier = Modifier.heightIn(min = GanAppSpacing.xxl)) {
                        Text("Reintentar")
                    }
                }
            }
            is AnimalScheduleLoad.Empty -> item {
                Text("No hay plantillas disponibles.")
            }
            is AnimalScheduleLoad.Ready -> items(load.templates, key = { it.idPlantilla }) { template ->
                TemplateChoiceCard(
                    template = template,
                    selected = template.idPlantilla == state.selectedTemplateId,
                    enabled = !state.isSaving,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigureScheduleContent(
    state: AnimalNotificationScheduleUiState,
    padding: PaddingValues,
    onStartDate: (LocalDate) -> Unit,
    onEndDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
    onRecurrence: (RecurrenciaNotificacion) -> Unit,
) {
    val context = LocalContext.current
    val animal = state.animal
    val template = state.selectedTemplate
    val draft = state.draft
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(GanAppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
    ) {
        if (animal != null) AnimalContextCard(animal)
        if (template != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(GanAppSpacing.md), verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs)) {
                    Text("Plantilla", style = MaterialTheme.typography.labelLarge)
                    Text(template.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(template.tipo.toPersistentString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        NotificationFormSectionTitle("Programación")
        NotificationScheduleField(
            label = "Fecha inicio",
            value = draft.fechaInicio?.format(DateFormat).orEmpty(),
            placeholder = "Seleccionar fecha inicio",
            icon = Icons.Outlined.CalendarMonth,
            onClick = {
                if (state.isSaving) return@NotificationScheduleField
                openDatePicker(context, draft.fechaInicio, onStartDate)
            },
        )
        FieldError(draft.fieldErrors[ScheduleField.FechaInicio])
        NotificationScheduleField(
            label = "Fecha fin",
            value = draft.fechaFin?.format(DateFormat).orEmpty(),
            placeholder = "Seleccionar fecha fin",
            icon = Icons.Outlined.CalendarMonth,
            onClick = {
                if (state.isSaving) return@NotificationScheduleField
                openDatePicker(context, draft.fechaFin, onEndDate)
            },
        )
        FieldError(draft.fieldErrors[ScheduleField.FechaFin])
        NotificationScheduleField(
            label = "Hora",
            value = draft.hora?.format(TimeFormat).orEmpty(),
            placeholder = "Seleccionar hora",
            icon = Icons.Outlined.Schedule,
            onClick = {
                if (state.isSaving) return@NotificationScheduleField
                openTimePicker(context, draft.hora, onTime)
            },
        )
        FieldError(draft.fieldErrors[ScheduleField.Hora])
        Text(
            "Recurrencia",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
        ) {
            RecurrenciaNotificacion.entries.forEach { value ->
                FilterChip(
                    selected = draft.recurrencia == value,
                    onClick = { onRecurrence(value) },
                    label = { Text(value.toPersistentString()) },
                    enabled = !state.isSaving,
                    modifier = Modifier.heightIn(min = GanAppSpacing.xxl),
                )
            }
        }
        if (state.isSaving) {
            CircularProgressIndicator(Modifier.semantics { contentDescription = "Guardando programación" })
        }
    }
}

@Composable
private fun AnimalContextCard(animal: Animal) {
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
private fun TemplateChoiceCard(
    template: PlantillaNotificacion,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
) {
    val description = if (selected) {
        "Plantilla seleccionada: ${template.titulo}"
    } else {
        "Plantilla: ${template.titulo}"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GanAppSpacing.xxl)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = { onSelect(template.idPlantilla) },
            ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            Modifier.padding(GanAppSpacing.md).semantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
        ) {
            Text(template.titulo, style = MaterialTheme.typography.titleMedium)
            Text(template.tipo.toPersistentString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FieldError(message: String?) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

private fun openDatePicker(
    context: android.content.Context,
    current: LocalDate?,
    onDate: (LocalDate) -> Unit,
) {
    val initial = current ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}

private fun openTimePicker(
    context: android.content.Context,
    current: LocalTime?,
    onTime: (LocalTime) -> Unit,
) {
    val initial = current ?: LocalTime.now()
    TimePickerDialog(
        context,
        { _, hour, minute -> onTime(LocalTime.of(hour, minute)) },
        initial.hour,
        initial.minute,
        true,
    ).show()
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
