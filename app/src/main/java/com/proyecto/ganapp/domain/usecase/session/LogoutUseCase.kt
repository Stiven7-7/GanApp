package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.usecase.notificacion.CancelAuthenticatedUserScheduleAlarmsResult
import com.proyecto.ganapp.domain.usecase.notificacion.CancelAuthenticatedUserScheduleAlarmsUseCase
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val cancelAuthenticatedUserScheduleAlarmsUseCase:
        CancelAuthenticatedUserScheduleAlarmsUseCase,
    private val clearSessionUseCase: ClearSessionUseCase,
) {
    suspend operator fun invoke(): LogoutResult {
        val cancellation = cancelAuthenticatedUserScheduleAlarmsUseCase()
        val clearSession = when (cancellation) {
            CancelAuthenticatedUserScheduleAlarmsResult.Cancelled,
            CancelAuthenticatedUserScheduleAlarmsResult.NoAuthenticatedUser,
            -> true
            is CancelAuthenticatedUserScheduleAlarmsResult.Failure -> false
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
