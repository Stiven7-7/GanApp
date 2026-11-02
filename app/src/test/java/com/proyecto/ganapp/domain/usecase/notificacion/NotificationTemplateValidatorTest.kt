package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.TipoNotificacion
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTemplateValidatorTest {
    private fun create(title: String) =
        CreateNotificationTemplateRequest(title, TipoNotificacion.VACUNACION)

    @Test
    fun tituloVacioOSoloEspacios_esInvalidoEnCreateYUpdate() {
        listOf("", "   ").forEach { title ->
            val expected = TemplateValidationResult.Invalid(TemplateValidationError.INVALID_TITLE)
            assertEquals(expected, NotificationTemplateValidator.validate(create(title)))
            assertEquals(expected, NotificationTemplateValidator.validate(
                UpdateNotificationTemplateRequest(1L, title, TipoNotificacion.VACUNACION),
            ))
        }
    }

    @Test
    fun tituloValido_seNormalizaSinCambiarMayusculas() {
        val expected = TemplateValidationResult.Valid("Vacuna Mixta")
        assertEquals(expected, NotificationTemplateValidator.validate(create("  Vacuna Mixta  ")))
        assertEquals(expected, NotificationTemplateValidator.validate(
            UpdateNotificationTemplateRequest(1L, "  Vacuna Mixta  ", TipoNotificacion.VACUNACION),
        ))
    }

    @Test
    fun requestsDuplicados_sonValidosIndependientemente() {
        val first = create("Vacuna")
        val second = create("Vacuna")
        assertEquals(TemplateValidationResult.Valid("Vacuna"), NotificationTemplateValidator.validate(first))
        assertEquals(TemplateValidationResult.Valid("Vacuna"), NotificationTemplateValidator.validate(second))
    }

    @Test
    fun updateIdInvalido_tienePrioridadSobreTitulo() {
        listOf(0L, -1L).forEach { id ->
            assertEquals(
                TemplateValidationResult.Invalid(TemplateValidationError.INVALID_TEMPLATE_ID),
                NotificationTemplateValidator.validate(
                    UpdateNotificationTemplateRequest(id, "", TipoNotificacion.VACUNACION),
                ),
            )
        }
    }
}
