package com.proyecto.ganapp.ui.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.usecase.notificacion.CreateAndScheduleNotificationUseCase
import com.proyecto.ganapp.domain.usecase.notificacion.CreateNotificationRequest
import com.proyecto.ganapp.domain.usecase.notificacion.CreateNotificationResult
import com.proyecto.ganapp.domain.usecase.notificacion.GetNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val createAndScheduleNotificationUseCase: CreateAndScheduleNotificationUseCase,
) : ViewModel() {

    val nombre = MutableStateFlow("")
    val tipo = MutableStateFlow("")
    val fechaInicio = MutableStateFlow(0L)
    val fechaFin = MutableStateFlow(0L)
    val seRepite = MutableStateFlow("NO")
    val hora = MutableStateFlow("08:00")

    private val _notifications = MutableStateFlow<List<Notificacion>>(emptyList())
    val notifications: StateFlow<List<Notificacion>> = _notifications

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _event = MutableSharedFlow<NotificationCreateEvent>()
    val event = _event.asSharedFlow()

    fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            getNotificationsUseCase(userId).collect { _notifications.value = it }
        }
    }

    fun onSave(userId: Long) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val tipoNotificacion = TipoNotificacion.fromPersistentString(tipo.value)
                val recurrencia = RecurrenciaNotificacion.fromPersistentString(seRepite.value)
                if (tipoNotificacion == null || recurrencia == null) {
                    _event.emit(NotificationCreateEvent.ValidationFailure(emptySet()))
                    return@launch
                }
                val now = System.currentTimeMillis()
                val request = CreateNotificationRequest(
                    nombre = nombre.value,
                    tipo = tipoNotificacion,
                    fechaInicio = fechaInicio.value,
                    fechaFin = fechaFin.value,
                    hora = hora.value,
                    recurrencia = recurrencia,
                    idUsuario = userId,
                )
                when (
                    val result = createAndScheduleNotificationUseCase(
                        request = request,
                        now = now,
                    )
                ) {
                    is CreateNotificationResult.Success -> {
                        resetForm()
                        _event.emit(NotificationCreateEvent.Success)
                    }
                    is CreateNotificationResult.ValidationFailure -> {
                        _event.emit(NotificationCreateEvent.ValidationFailure(result.errors))
                    }
                    is CreateNotificationResult.PersistenceFailure -> {
                        _event.emit(NotificationCreateEvent.PersistenceFailure)
                    }
                    is CreateNotificationResult.SchedulingFailure -> {
                        _event.emit(
                            NotificationCreateEvent.SchedulingFailure(result.notificationId),
                        )
                    }
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun resetForm() {
        nombre.value = ""
        tipo.value = ""
        fechaInicio.value = 0L
        fechaFin.value = 0L
        seRepite.value = "NO"
        hora.value = "08:00"
    }
}
