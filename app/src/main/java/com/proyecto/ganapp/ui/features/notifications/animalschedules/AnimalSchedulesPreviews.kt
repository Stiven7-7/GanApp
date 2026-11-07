package com.proyecto.ganapp.ui.features.notifications.animalschedules

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.ui.common.theme.GanAppTheme
import java.time.LocalDate
import java.time.LocalTime

private const val PreviewWidth = 320
private const val PreviewHeight = 640

@Preview(name = "Lista", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Lista 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewAnimalSchedulesList() {
    SchedulesPreviewHost(AnimalSchedulesContent.Success(previewAnimal(), previewDetails()))
}

@Preview(name = "Vacío", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Vacío 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewAnimalSchedulesEmpty() {
    SchedulesPreviewHost(AnimalSchedulesContent.Empty(previewAnimal()))
}

@Preview(name = "Cargando", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Cargando 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewAnimalSchedulesLoading() {
    SchedulesPreviewHost(AnimalSchedulesContent.Loading)
}

@Preview(name = "Error", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 1f)
@Preview(name = "Error 2.0", widthDp = PreviewWidth, heightDp = PreviewHeight, fontScale = 2f)
@Composable
fun PreviewAnimalSchedulesError() {
    SchedulesPreviewHost(AnimalSchedulesContent.Error(AnimalSchedulesErrorMessage))
}

@Composable
private fun SchedulesPreviewHost(state: AnimalSchedulesContent) {
    GanAppTheme {
        AnimalSchedulesScreen(state = state, onBack = {}, onRetry = {})
    }
}

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

private fun previewDetails(): List<ProgramacionNotificacionDetalle> {
    val template = PlantillaNotificacion(
        idPlantilla = 5L,
        titulo = "Revisión de ciclo reproductivo del hato lechero",
        tipo = TipoNotificacion.CICLO_REPRODUCTIVO,
        idUsuario = 7L,
    )
    return listOf(
        ProgramacionNotificacionDetalle(
            programacion = previewSchedule(77L, LocalTime.of(8, 0), RecurrenciaNotificacion.DIARIA),
            plantilla = template,
        ),
        ProgramacionNotificacionDetalle(
            programacion = previewSchedule(78L, LocalTime.of(18, 30), RecurrenciaNotificacion.SEMANAL),
            plantilla = template,
        ),
    )
}

private fun previewSchedule(id: Long, hora: LocalTime, recurrencia: RecurrenciaNotificacion) =
    ProgramacionNotificacion(
        idProgramacion = id,
        idPlantilla = 5L,
        idAnimal = 9L,
        fechaInicio = LocalDate.of(2026, 11, 15),
        fechaFin = LocalDate.of(2026, 11, 30),
        hora = hora,
        recurrencia = recurrencia,
    )
