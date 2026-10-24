package com.proyecto.ganapp.domain.usecase.notificacion

enum class CreateNotificationValidationError {
    BLANK_NAME,
    INVALID_USER_ID,
    INVALID_START_DATE,
    INVALID_END_DATE,
    END_BEFORE_START,
    NON_RECURRING_END_MUST_EQUAL_START,
    INVALID_TIME,
    FIRST_OCCURRENCE_NOT_FUTURE,
}
