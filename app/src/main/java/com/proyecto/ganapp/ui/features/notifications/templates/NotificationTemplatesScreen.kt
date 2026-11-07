package com.proyecto.ganapp.ui.features.notifications.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

/** Entrada aislada. Requiere ViewModel explícito; no resuelve dependencias productivas. */
@Composable
fun NotificationTemplatesRoute(viewModel: NotificationTemplatesViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) { is TemplateEvent.Message -> snackbar.showSnackbar(event.text) }
        }
    }
    BackHandler(enabled = state.editor != null) { viewModel.dismissEditor() }
    NotificationTemplatesScreen(
        state, snackbar, onBack = { if (state.editor != null) viewModel.dismissEditor() else onBack() },
        onRetry = viewModel::load, onCreate = viewModel::openCreate, onEdit = viewModel::openEdit,
        onDelete = viewModel::requestDelete, onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete, onTitleChange = viewModel::changeTitle,
        onTypeChange = viewModel::changeType, onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationTemplatesScreen(
    state: NotificationTemplatesUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (PlantillaNotificacion) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTypeChange: (TipoNotificacion) -> Unit,
    onSave: () -> Unit,
) {
    val editor = state.editor
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(
                    if (editor == null) "Plantillas de notificación"
                    else if (editor.templateId == null) "Nueva plantilla" else "Editar plantilla",
                    textAlign = TextAlign.Center,
                ) },
                expandedHeight = if (LocalDensity.current.fontScale >= 1.5f) GanAppSpacing.xxl * 3
                    else TopAppBarDefaults.TopAppBarExpandedHeight,
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isWorking) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = ganAppTopAppBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (editor == null) ExtendedFloatingActionButton(
                onClick = { if (!state.isWorking) onCreate() },
            ) { Icon(Icons.Default.Add, contentDescription = null); Text("Nueva plantilla") }
        },
    ) { padding ->
        if (editor != null) {
            Column(
                Modifier.padding(padding).fillMaxSize().imePadding()
                    .verticalScroll(rememberScrollState()).padding(GanAppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
            ) {
                OutlinedTextField(
                    value = editor.form.titulo, onValueChange = onTitleChange,
                    label = { Text("Título") }, enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Tipo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
                ) {
                    TipoNotificacion.entries.forEach { type ->
                        FilterChip(
                            selected = editor.form.tipo == type, onClick = { onTypeChange(type) },
                            label = { Text(type.toPersistentString()) }, enabled = !state.isWorking,
                        )
                    }
                }
                Button(onClick = onSave, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                    Text(if (editor.templateId == null) "Guardar plantilla" else "Guardar cambios")
                }
                if (state.isWorking) CircularProgressIndicator(Modifier.semantics { contentDescription = "Guardando plantilla" })
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = GanAppSpacing.md, end = GanAppSpacing.md, top = GanAppSpacing.md,
                    bottom = 160.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
            ) {
                when (val content = state.content) {
                    TemplateListState.Loading -> item {
                        CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando plantillas" })
                    }
                    TemplateListState.Empty -> item { Text("No tienes plantillas. Crea una para comenzar.") }
                    is TemplateListState.Error -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                            Text(content.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = onRetry) { Text("Reintentar") }
                        }
                    }
                    is TemplateListState.Success -> items(content.templates, key = { it.idPlantilla }) { template ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(GanAppSpacing.md), verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs)) {
                                Text(template.titulo, style = MaterialTheme.typography.titleMedium)
                                Text(template.tipo.toPersistentString(), style = MaterialTheme.typography.bodyMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs)) {
                                    TextButton(onClick = { onEdit(template.idPlantilla) }, enabled = !state.isWorking,
                                        modifier = Modifier.semantics { contentDescription = "Editar plantilla: " + template.titulo }) {
                                        Text("Editar")
                                    }
                                    TextButton(onClick = { onDelete(template) }, enabled = !state.isWorking,
                                        modifier = Modifier.semantics { contentDescription = "Eliminar plantilla: " + template.titulo }) {
                                        Text("Eliminar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    state.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Eliminar plantilla") },
            text = { Text("¿Quieres eliminar la plantilla “" + target.titulo + "”?") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete, enabled = !state.isWorking) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete, enabled = !state.isWorking) { Text("Cancelar") }
            },
        )
    }
}
