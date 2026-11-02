package com.proyecto.ganapp.domain.model

/** Modelo de lectura; conserva la plantilla actual sin snapshots. */
data class ProgramacionNotificacionDetalle(
    val programacion: ProgramacionNotificacion,
    val plantilla: PlantillaNotificacion,
)
