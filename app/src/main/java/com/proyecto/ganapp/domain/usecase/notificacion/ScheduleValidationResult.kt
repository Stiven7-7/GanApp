package com.proyecto.ganapp.domain.usecase.notificacion

enum class ScheduleValidationError {
    INVALID_TEMPLATE_ID,
    INVALID_ANIMAL_ID,
    INVALID_SCHEDULE_ID,
    INVALID_TIME_PRECISION,
    END_BEFORE_START,
    NO_REQUIRES_SAME_END_DATE,
    NO_FUTURE_OCCURRENCE,
}

sealed interface ScheduleValidationResult {
    data class Valid(val nextOccurrenceAtMillis: Long) : ScheduleValidationResult
    data class Invalid(val error: ScheduleValidationError) : ScheduleValidationResult
}
