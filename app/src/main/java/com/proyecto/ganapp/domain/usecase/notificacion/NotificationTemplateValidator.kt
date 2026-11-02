package com.proyecto.ganapp.domain.usecase.notificacion

object NotificationTemplateValidator {
    fun validate(request: CreateNotificationTemplateRequest): TemplateValidationResult =
        validateTitle(request.titulo)

    fun validate(request: UpdateNotificationTemplateRequest): TemplateValidationResult {
        if (request.templateId <= 0L) {
            return TemplateValidationResult.Invalid(TemplateValidationError.INVALID_TEMPLATE_ID)
        }
        return validateTitle(request.titulo)
    }

    private fun validateTitle(titulo: String): TemplateValidationResult {
        val normalized = titulo.trim()
        return if (normalized.isEmpty()) {
            TemplateValidationResult.Invalid(TemplateValidationError.INVALID_TITLE)
        } else {
            TemplateValidationResult.Valid(normalized)
        }
    }
}
