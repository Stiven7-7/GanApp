package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.usecase.notificacion.CancelAuthenticatedUserNotificationAlarmsResult
import com.proyecto.ganapp.domain.usecase.notificacion.CancelAuthenticatedUserNotificationAlarmsUseCase
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val cancelAuthenticatedUserNotificationAlarmsUseCase:
        CancelAuthenticatedUserNotificationAlarmsUseCase,
    private val clearSessionUseCase: ClearSessionUseCase,
) {
    suspend operator fun invoke(): LogoutResult {
        val cancellation = cancelAuthenticatedUserNotificationAlarmsUseCase()
        val clearSession = when (cancellation) {
            is CancelAuthenticatedUserNotificationAlarmsResult.Cancelled ->
                cancellation.failed == 0
            CancelAuthenticatedUserNotificationAlarmsResult.NoAuthenticatedUser ->
                true
            is CancelAuthenticatedUserNotificationAlarmsResult.Failure ->
                false
        }
        if (!clearSession) {
            return LogoutResult.AlarmCancellationFailure
        }
        return when (clearSessionUseCase()) {
            ClearSessionResult.Success -> LogoutResult.Success
            ClearSessionResult.UnexpectedError -> LogoutResult.SessionClearFailure
        }
    }
}
