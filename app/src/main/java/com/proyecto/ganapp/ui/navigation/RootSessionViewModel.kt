package com.proyecto.ganapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.usecase.notificacion.ReconcileNotificationScheduleAlarmsUseCase
import com.proyecto.ganapp.domain.usecase.session.ObserveSessionResult
import com.proyecto.ganapp.domain.usecase.session.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootSessionViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    private val reconcileNotificationScheduleAlarmsUseCase: ReconcileNotificationScheduleAlarmsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RootSessionState>(RootSessionState.Loading)
    val uiState: StateFlow<RootSessionState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { result ->
                _uiState.value = when (result) {
                    ObserveSessionResult.Unauthenticated -> RootSessionState.Unauthenticated
                    is ObserveSessionResult.Authenticated -> {
                        reconcileCurrentSession()
                        RootSessionState.Authenticated(userId = result.userId)
                    }
                }
            }
        }
    }

    private suspend fun reconcileCurrentSession() {
        try {
            reconcileNotificationScheduleAlarmsUseCase(
                now = System.currentTimeMillis(),
                zoneId = ZoneId.systemDefault(),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Unit
        }
    }
}
