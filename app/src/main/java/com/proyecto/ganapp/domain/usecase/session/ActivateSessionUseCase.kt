package com.proyecto.ganapp.domain.usecase.session

import com.proyecto.ganapp.domain.usecase.notificacion.ReconcileNotificationScheduleAlarmsResult
import com.proyecto.ganapp.domain.usecase.notificacion.ReconcileNotificationScheduleAlarmsUseCase
import java.time.ZoneId
import javax.inject.Inject

class ActivateSessionUseCase @Inject constructor(
    private val saveSessionUseCase: SaveSessionUseCase,
    private val reconcileNotificationScheduleAlarmsUseCase: ReconcileNotificationScheduleAlarmsUseCase,
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
            val reconciled = reconcileNotificationScheduleAlarmsUseCase(
                now = now,
                zoneId = zoneId,
            )
        ) {
            is ReconcileNotificationScheduleAlarmsResult.Reconciled ->
                if (reconciled.failed == 0) {
                    ActivateSessionResult.Success
                } else {
                    ActivateSessionResult.SuccessWithNotificationReconciliationFailure
                }
            ReconcileNotificationScheduleAlarmsResult.NoAuthenticatedUser,
            is ReconcileNotificationScheduleAlarmsResult.Failure,
            -> ActivateSessionResult.SuccessWithNotificationReconciliationFailure
        }
    }
}
