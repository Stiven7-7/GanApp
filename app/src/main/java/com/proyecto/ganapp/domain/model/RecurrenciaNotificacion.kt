package com.proyecto.ganapp.domain.model

enum class RecurrenciaNotificacion {
    NO,
    DIARIA,
    SEMANAL,
    CADA_15_DIAS,
    MENSUAL,
    ANUAL,
    ;

    fun toPersistentString(): String = when (this) {
        NO -> "NO"
        DIARIA -> "DIARIA"
        SEMANAL -> "SEMANAL"
        CADA_15_DIAS -> "CADA 15 DÍAS"
        MENSUAL -> "MENSUAL"
        ANUAL -> "ANUAL"
    }

    companion object {
        fun fromPersistentString(value: String): RecurrenciaNotificacion? = when (value) {
            "NO" -> NO
            "DIARIA" -> DIARIA
            "SEMANAL" -> SEMANAL
            "CADA 15 DÍAS", "CADA 15 DIAS" -> CADA_15_DIAS
            "MENSUAL" -> MENSUAL
            "ANUAL" -> ANUAL
            else -> null
        }
    }
}
