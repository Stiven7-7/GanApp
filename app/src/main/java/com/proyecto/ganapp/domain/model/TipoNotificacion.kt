package com.proyecto.ganapp.domain.model

enum class TipoNotificacion {
    VACUNACION,
    ALIMENTACION,
    CICLO_REPRODUCTIVO,
    ;

    fun toPersistentString(): String = when (this) {
        VACUNACION -> "Vacunación"
        ALIMENTACION -> "Alimentación"
        CICLO_REPRODUCTIVO -> "Ciclo Reproductivo"
    }

    companion object {
        fun fromPersistentString(value: String): TipoNotificacion? = when (value) {
            "Vacunación" -> VACUNACION
            "Alimentación" -> ALIMENTACION
            "Ciclo Reproductivo" -> CICLO_REPRODUCTIVO
            else -> null
        }
    }
}
