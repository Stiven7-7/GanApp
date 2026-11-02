package com.proyecto.ganapp.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** idAnimal null se reserva para compatibilidad legacy migrada. */
data class ProgramacionNotificacion(
    val idProgramacion: Long = 0L,
    val idPlantilla: Long,
    val idAnimal: Long?,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
    /** Hora válida con precisión de minuto; comprobada por los validators. */
    val hora: LocalTime,
    val recurrencia: RecurrenciaNotificacion,
)
