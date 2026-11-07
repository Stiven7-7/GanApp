package com.proyecto.ganapp.ui.features.notifications.templates

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion

sealed interface TemplateListState {
    data object Loading : TemplateListState
    data object Empty : TemplateListState
    data class Error(val message: String) : TemplateListState
    data class Success(val templates: List<PlantillaNotificacion>) : TemplateListState
}

/** El formulario contiene exclusivamente los datos editables de una plantilla. */
data class TemplateForm(
    val titulo: String = "",
    val tipo: TipoNotificacion = TipoNotificacion.VACUNACION,
)

data class TemplateEditor(
    val templateId: Long? = null,
    val form: TemplateForm = TemplateForm(),
)

data class NotificationTemplatesUiState(
    val content: TemplateListState = TemplateListState.Loading,
    val editor: TemplateEditor? = null,
    val deleteTarget: PlantillaNotificacion? = null,
    val isWorking: Boolean = false,
)

sealed interface TemplateEvent {
    data class Message(val text: String) : TemplateEvent
}
