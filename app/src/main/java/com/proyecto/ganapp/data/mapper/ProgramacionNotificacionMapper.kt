package com.proyecto.ganapp.data.mapper

import com.proyecto.ganapp.data.local.entity.ProgramacionNotificacionEntity
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

private val scheduleMinuteFormat =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
private val strictScheduleTime = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")

/** Compartido con la preparación de migración: rechaza texto sin corregirlo. */
internal fun parseNotificationScheduleTime(value: String): LocalTime {
    require(strictScheduleTime.matches(value)) { "Invalid notification schedule time: $value" }
    return LocalTime.parse(value, scheduleMinuteFormat)
}

fun ProgramacionNotificacionEntity.toDomain(): ProgramacionNotificacion =
    ProgramacionNotificacion(
        idProgramacion = idProgramacion,
        idPlantilla = idPlantilla,
        idAnimal = idAnimal,
        fechaInicio = LocalDate.ofEpochDay(fechaInicio),
        fechaFin = LocalDate.ofEpochDay(fechaFin),
        hora = parseNotificationScheduleTime(hora),
        recurrencia = requireNotNull(RecurrenciaNotificacion.fromPersistentString(recurrencia)) {
            "Unknown notification schedule recurrence: $recurrencia"
        },
    )

fun ProgramacionNotificacion.toEntity(): ProgramacionNotificacionEntity {
    require(hora.second == 0 && hora.nano == 0) { "Notification schedule time must have minute precision" }
    return ProgramacionNotificacionEntity(
        idProgramacion = idProgramacion,
        idPlantilla = idPlantilla,
        idAnimal = idAnimal,
        fechaInicio = fechaInicio.toEpochDay(),
        fechaFin = fechaFin.toEpochDay(),
        hora = hora.format(scheduleMinuteFormat),
        recurrencia = recurrencia.toPersistentString(),
    )
}
