package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.usecase.notificacion.ReconcileNotificationAlarmsResult
import com.proyecto.ganapp.domain.usecase.notificacion.ReconcileNotificationAlarmsUseCase
import java.time.ZoneId
import javax.inject.Inject

class ActivateSessionUseCase @Inject constructor(
    private val saveSessionUseCase: SaveSessionUseCase,
    private val reconcileNotificationAlarmsUseCase: ReconcileNotificationAlarmsUseCase,
) {
    suspend operator fun invoke(
        userId: Long,
        now: Long,
        zoneId: ZoneId,
    ): ActivateSessionResult {
        when (saveSessionUseCase(userId)) {
            SaveSessionResult.InvalidUserId ->
                return ActivateSessionResult.InvalidUserId
            SaveSessionResult.UnexpectedError ->
                return ActivateSessionResult.SessionSaveFailure
            SaveSessionResult.Success -> Unit
        }
        return when (
            val reconciled = reconcileNotificationAlarmsUseCase(
                now = now,
                zoneId = zoneId,
            )
        ) {
            is ReconcileNotificationAlarmsResult.Reconciled ->
                if (reconciled.failed == 0) {
                    ActivateSessionResult.Success
                } else {
                    ActivateSessionResult.SuccessWithNotificationReconciliationFailure
                }
            ReconcileNotificationAlarmsResult.NoAuthenticatedUser,
            is ReconcileNotificationAlarmsResult.Failure,
            -> ActivateSessionResult.SuccessWithNotificationReconciliationFailure
        }
    }
}
