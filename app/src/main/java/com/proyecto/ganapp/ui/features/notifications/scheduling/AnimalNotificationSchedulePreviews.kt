package com.proyecto.ganapp.ui.features.notifications.scheduling

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.ui.common.theme.GanAppTheme
import java.time.LocalDate
import java.time.LocalTime

private const val PreviewWidth = 320
private const val PreviewHeight = 640

@Preview(name = "Seleccionar", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Seleccionar 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewAnimalScheduleSelect() {
    SchedulePreviewHost(
        AnimalNotificationScheduleUiState(
            load = AnimalScheduleLoad.Ready(previewAnimal(), previewTemplates()),
            selectedTemplateId = 2L,
        ),
    )
}

@Preview(name = "Vacío", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Vacío 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewAnimalScheduleEmpty() {
    SchedulePreviewHost(
        AnimalNotificationScheduleUiState(load = AnimalScheduleLoad.Empty(previewAnimal())),
    )
}

@Preview(name = "Configurar", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Configurar 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewAnimalScheduleConfigure() {
    SchedulePreviewHost(previewConfigureState())
}

@Preview(name = "Errores", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Errores 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewAnimalScheduleErrors() {
    SchedulePreviewHost(
        previewConfigureState(
            draft = ScheduleDraft(
                fechaInicio = LocalDate.of(2026, 11, 20),
                fechaFin = LocalDate.of(2026, 11, 15),
                hora = LocalTime.of(9, 0),
                recurrencia = RecurrenciaNotificacion.DIARIA,
                fieldErrors = mapOf(
                    ScheduleField.FechaFin to "La fecha fin no puede ser anterior a la fecha inicio.",
                ),
            ),
        ),
    )
}

@Composable
private fun SchedulePreviewHost(state: AnimalNotificationScheduleUiState) {
    GanAppTheme {
        AnimalNotificationScheduleScreen(
            state = state,
            snackbar = remember { SnackbarHostState() },
            onBack = {},
            onRetry = {},
            onSelect = {},
            onContinue = {},
            onStartDate = {},
            onEndDate = {},
            onTime = {},
            onRecurrence = {},
            onSave = {},
        )
    }
}

private fun previewConfigureState(draft: ScheduleDraft = ScheduleDraft(
    fechaInicio = LocalDate.of(2026, 11, 15),
    fechaFin = LocalDate.of(2026, 11, 20),
    hora = LocalTime.of(9, 0),
    recurrencia = RecurrenciaNotificacion.DIARIA,
)) = AnimalNotificationScheduleUiState(
    load = AnimalScheduleLoad.Ready(previewAnimal(), previewTemplates()),
    selectedTemplateId = 2L,
    step = AnimalScheduleStep.Configure,
    draft = draft,
)

private fun previewAnimal() = Animal(
    idAnimal = 9L,
    especie = EspecieAnimal.VACA,
    nombre = "Mancha",
    raza = "Holstein",
    edad = null,
    peso = null,
    color = null,
    fotoUri = null,
    idUsuario = 7L,
)

private fun previewTemplates() = listOf(
    PlantillaNotificacion(1L, "Vacuna aftosa", TipoNotificacion.VACUNACION, 7L),
    PlantillaNotificacion(2L, "Suplemento mineral de la mañana", TipoNotificacion.ALIMENTACION, 7L),
    PlantillaNotificacion(3L, "Revisión de ciclo reproductivo del hato", TipoNotificacion.CICLO_REPRODUCTIVO, 7L),
)
