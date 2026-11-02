package com.proyecto.ganapp.domain.usecase.notificacion

enum class TemplateValidationError {
    INVALID_TEMPLATE_ID,
    INVALID_TITLE,
}

sealed interface TemplateValidationResult {
    data class Valid(val titulo: String) : TemplateValidationResult
    data class Invalid(val error: TemplateValidationError) : TemplateValidationResult
}
