package com.proyecto.ganapp.ui.features.notifications.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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

@OptIn(ExperimentalMaterial3Api::class)
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(GanAppSpacing.xs))
                Text("Nueva plantilla")
            }
        },
    ) { padding ->
        if (editor != null) {
            TemplateEditorForm(
                editor = editor,
                isWorking = state.isWorking,
                onTitleChange = onTitleChange,
                onTypeChange = onTypeChange,
                onSave = onSave,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .padding(bottom = GanAppSpacing.xxl * 2 + GanAppSpacing.lg)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = GanAppSpacing.md,
                    end = GanAppSpacing.md,
                    top = GanAppSpacing.md,
                    bottom = GanAppSpacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
            ) {
                item { TemplatesListHeader() }
                when (val content = state.content) {
                    TemplateListState.Loading -> item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = GanAppSpacing.xl),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                Modifier.semantics { contentDescription = "Cargando plantillas" },
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    TemplateListState.Empty -> item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = GanAppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
                        ) {
                            Icon(
                                Icons.Outlined.FactCheck,
                                contentDescription = null,
                                modifier = Modifier.size(GanAppSpacing.xl),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "No tienes plantillas. Crea una para comenzar.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    is TemplateListState.Error -> item {
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
                        ) {
                            Text(
                                content.message,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GanAppSpacing.md),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("Reintentar") }
                        }
                    }
                    is TemplateListState.Success -> items(content.templates, key = { it.idPlantilla }) { template ->
                        TemplateCard(
                            template = template,
                            enabled = !state.isWorking,
                            onEdit = { onEdit(template.idPlantilla) },
                            onDelete = { onDelete(template) },
                        )
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
                TextButton(
                    onClick = onConfirmDelete,
                    enabled = !state.isWorking,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete, enabled = !state.isWorking) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun TemplatesListHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
        Text(
            "GESTIÓN DE ALERTAS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Plantillas activas",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.padding(GanAppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(GanAppSpacing.xxl),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs)) {
                    Text("Automatiza tu calendario", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Define recordatorios reutilizables para aplicar a tus animales en lote o individualmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateCard(
    template: PlantillaNotificacion,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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
                TemplateTypeBadge(type = template.tipo, onPrimarySurface = false)
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
                ) {
                    Text(
                        template.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TemplateTypeLabel(template.tipo)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xs),
            ) {
                TextButton(
                    onClick = onEdit,
                    enabled = enabled,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Editar plantilla: " + template.titulo },
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(GanAppSpacing.xs))
                    Text("Editar")
                }
                TextButton(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Eliminar plantilla: " + template.titulo },
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
}

@Composable
private fun TemplateTypeLabel(type: TipoNotificacion) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            type.toPersistentString(),
            modifier = Modifier.padding(horizontal = GanAppSpacing.sm, vertical = GanAppSpacing.xxs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun TemplateEditorForm(
    editor: TemplateEditor,
    isWorking: Boolean,
    onTitleChange: (String) -> Unit,
    onTypeChange: (TipoNotificacion) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(GanAppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier.padding(GanAppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(GanAppSpacing.xxl),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.FactCheck, contentDescription = null)
                    }
                }
                Text(
                    "Configura una plantilla reutilizable para registrar eventos en tu rebaño.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        OutlinedTextField(
            value = editor.form.titulo,
            onValueChange = onTitleChange,
            label = { Text("Título") },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        Text(
            "Tipo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
            TipoNotificacion.entries.forEach { type ->
                TemplateTypeOption(
                    type = type,
                    selected = editor.form.tipo == type,
                    enabled = !isWorking,
                    onClick = { onTypeChange(type) },
                )
            }
        }
        Button(
            onClick = onSave,
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(if (editor.templateId == null) "Guardar plantilla" else "Guardar cambios")
        }
        if (isWorking) {
            CircularProgressIndicator(
                Modifier.semantics { contentDescription = "Guardando plantilla" },
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "La plantilla quedará lista para asignar a cualquier animal en tu finca.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TemplateTypeOption(
    type: TipoNotificacion,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(GanAppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(GanAppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TemplateTypeBadge(type = type, onPrimarySurface = selected)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GanAppSpacing.xxs),
            ) {
                Text(type.toPersistentString(), style = MaterialTheme.typography.titleMedium)
                Text(
                    type.supportLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) content else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TemplateTypeBadge(type: TipoNotificacion, onPrimarySurface: Boolean) {
    Surface(
        modifier = Modifier.size(GanAppSpacing.xxl),
        shape = CircleShape,
        color = if (onPrimarySurface) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (onPrimarySurface) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(type.icon(), contentDescription = null)
        }
    }
}

private fun TipoNotificacion.icon(): ImageVector = when (this) {
    TipoNotificacion.VACUNACION -> Icons.Outlined.Vaccines
    TipoNotificacion.ALIMENTACION -> Icons.Outlined.Grass
    TipoNotificacion.CICLO_REPRODUCTIVO -> Icons.Outlined.Autorenew
}

private fun TipoNotificacion.supportLabel(): String = when (this) {
    TipoNotificacion.VACUNACION -> "Dosis, refuerzos y antiparasitarios"
    TipoNotificacion.ALIMENTACION -> "Suplementos, ensilaje y pastoreo"
    TipoNotificacion.CICLO_REPRODUCTIVO -> "Inseminación, palpación y partos"
}
