package com.proyecto.ganapp.ui.features.notifications.templates

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.ui.common.theme.GanAppTheme

private const val PreviewWidth = 320
private const val PreviewHeight = 640

@Preview(name = "Lista", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Lista 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewNotificationTemplatesList() {
    TemplatesPreviewHost(sampleListState())
}

@Preview(name = "Vacío", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Vacío 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewNotificationTemplatesEmpty() {
    TemplatesPreviewHost(NotificationTemplatesUiState(content = TemplateListState.Empty))
}

@Preview(name = "Crear", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Crear 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewNotificationTemplatesCreate() {
    TemplatesPreviewHost(
        NotificationTemplatesUiState(
            content = TemplateListState.Empty,
            editor = TemplateEditor(form = TemplateForm(titulo = "Vacuna aftosa")),
        ),
    )
}

@Preview(name = "Editar", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Editar 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewNotificationTemplatesEdit() {
    TemplatesPreviewHost(
        NotificationTemplatesUiState(
            content = sampleListState().content,
            editor = TemplateEditor(
                templateId = 2L,
                form = TemplateForm(
                    titulo = "Suplemento mineral",
                    tipo = TipoNotificacion.ALIMENTACION,
                ),
            ),
        ),
    )
}

@Preview(name = "Cargando", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Cargando 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewNotificationTemplatesLoading() {
    TemplatesPreviewHost(NotificationTemplatesUiState(content = TemplateListState.Loading))
}

@Preview(name = "Error", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Error 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewNotificationTemplatesError() {
    TemplatesPreviewHost(
        NotificationTemplatesUiState(
            content = TemplateListState.Error("No se pudieron cargar las plantillas."),
        ),
    )
}

@Preview(name = "Eliminar", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Eliminar 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewNotificationTemplatesDelete() {
    TemplatesPreviewHost(
        sampleListState().copy(
            deleteTarget = previewTemplate(2L, "Suplemento mineral", TipoNotificacion.ALIMENTACION),
        ),
    )
}

@Composable
private fun TemplatesPreviewHost(state: NotificationTemplatesUiState) {
    GanAppTheme {
        NotificationTemplatesScreen(
            state = state,
            snackbar = remember { SnackbarHostState() },
            onBack = {},
            onRetry = {},
            onCreate = {},
            onEdit = {},
            onDelete = {},
            onDismissDelete = {},
            onConfirmDelete = {},
            onTitleChange = {},
            onTypeChange = {},
            onSave = {},
        )
    }
}

private fun sampleListState(): NotificationTemplatesUiState {
    return NotificationTemplatesUiState(
        content = TemplateListState.Success(
            listOf(
                previewTemplate(1L, "Vacuna aftosa", TipoNotificacion.VACUNACION),
                previewTemplate(2L, "Suplemento mineral de la mañana", TipoNotificacion.ALIMENTACION),
                previewTemplate(
                    3L,
                    "Revisión de ciclo reproductivo del hato",
                    TipoNotificacion.CICLO_REPRODUCTIVO,
                ),
                previewTemplate(4L, "Control de peso", TipoNotificacion.ALIMENTACION),
            ),
        ),
    )
}

private fun previewTemplate(
    id: Long,
    titulo: String,
    tipo: TipoNotificacion,
): PlantillaNotificacion {
    return PlantillaNotificacion(
        idPlantilla = id,
        titulo = titulo,
        tipo = tipo,
        idUsuario = 7L,
    )
}
