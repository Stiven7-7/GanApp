package com.proyecto.ganapp.ui.features.notifications.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.usecase.notificacion.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationTemplatesViewModel @Inject constructor(
    private val getTemplates: GetNotificationTemplatesUseCase,
    private val createTemplate: CreateNotificationTemplateUseCase,
    private val getTemplate: GetNotificationTemplateByIdUseCase,
    private val updateTemplate: UpdateNotificationTemplateUseCase,
    private val deleteTemplate: DeleteNotificationTemplateUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationTemplatesUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<TemplateEvent>()
    val events = _events.asSharedFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(content = TemplateListState.Loading) }
        loadJob = viewModelScope.launch {
            try {
                getTemplates().collect { templates ->
                    _uiState.update {
                        it.copy(content = if (templates.isEmpty()) TemplateListState.Empty
                        else TemplateListState.Success(templates))
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(content = TemplateListState.Error("No se pudieron cargar las plantillas.")) }
            }
        }
    }

    fun openCreate() {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(editor = TemplateEditor(), deleteTarget = null) }
    }

    fun openEdit(templateId: Long) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, deleteTarget = null) }
        viewModelScope.launch {
            try {
                when (val result = getTemplate(templateId)) {
                    is GetNotificationTemplateByIdResult.Success -> _uiState.update {
                        it.copy(editor = TemplateEditor(result.template.idPlantilla,
                            TemplateForm(result.template.titulo, result.template.tipo)))
                    }
                    else -> message("No se pudo abrir la plantilla.")
                }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    fun changeTitle(value: String) = changeForm { it.copy(titulo = value) }
    fun changeType(value: TipoNotificacion) = changeForm { it.copy(tipo = value) }

    private fun changeForm(transform: (TemplateForm) -> TemplateForm) {
        if (_uiState.value.isWorking) return
        _uiState.update { state -> state.copy(editor = state.editor?.let { it.copy(form = transform(it.form)) }) }
    }

    fun dismissEditor() {
        if (!_uiState.value.isWorking) _uiState.update { it.copy(editor = null) }
    }

    fun save() {
        val state = _uiState.value
        val editor = state.editor ?: return
        if (state.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            try {
                val id = editor.templateId
                val success = if (id == null) {
                    when (createTemplate(CreateNotificationTemplateRequest(editor.form.titulo, editor.form.tipo))) {
                        is CreateNotificationTemplateResult.Success -> true
                        is CreateNotificationTemplateResult.InvalidInput -> {
                            message("Escribe un título para la plantilla."); false
                        }
                        else -> { message("No se pudo guardar la plantilla."); false }
                    }
                } else {
                    when (updateTemplate(UpdateNotificationTemplateRequest(id, editor.form.titulo, editor.form.tipo))) {
                        UpdateNotificationTemplateResult.Success -> true
                        is UpdateNotificationTemplateResult.InvalidInput -> {
                            message("Escribe un título para la plantilla."); false
                        }
                        else -> { message("No se pudieron guardar los cambios."); false }
                    }
                }
                if (success) {
                    _uiState.update { it.copy(editor = null) }
                    message(if (id == null) "Plantilla guardada." else "Cambios guardados.")
                }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    fun requestDelete(template: PlantillaNotificacion) {
        if (!_uiState.value.isWorking) _uiState.update { it.copy(deleteTarget = template, editor = null) }
    }

    fun dismissDelete() {
        if (!_uiState.value.isWorking) _uiState.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val state = _uiState.value
        val target = state.deleteTarget ?: return
        if (state.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            try {
                when (deleteTemplate(target.idPlantilla)) {
                    DeleteNotificationTemplateResult.Success -> {
                        _uiState.update { it.copy(deleteTarget = null) }
                        message("Plantilla eliminada.")
                    }
                    DeleteNotificationTemplateResult.InUse -> {
                        _uiState.update { it.copy(deleteTarget = null) }
                        message("No se puede eliminar porque la plantilla está en uso.")
                    }
                    else -> message("No se pudo eliminar la plantilla.")
                }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    private suspend fun message(text: String) { _events.emit(TemplateEvent.Message(text)) }
}
