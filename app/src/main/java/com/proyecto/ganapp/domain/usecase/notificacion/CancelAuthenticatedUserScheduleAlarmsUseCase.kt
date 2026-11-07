package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.LegacyNotificationAlarmCanceller
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class CancelAuthenticatedUserScheduleAlarmsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val scheduleRepository: NotificationScheduleRepository,
    private val alarmService: NotificationScheduleAlarmService,
    private val legacyCanceller: LegacyNotificationAlarmCanceller,
) {
    suspend operator fun invoke(): CancelAuthenticatedUserScheduleAlarmsResult {
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) {
                return CancelAuthenticatedUserScheduleAlarmsResult.NoAuthenticatedUser
            }
            val templates = templateRepository.getTemplatesByUser(userId).first()
                .filter { it.idUsuario == userId && it.idPlantilla > 0L }
            for (template in templates) {
                if (!legacyCanceller.cancel(template.idPlantilla).succeeded()) {
                    return CancelAuthenticatedUserScheduleAlarmsResult.Failure()
                }
                for (schedule in scheduleRepository.getSchedulesByTemplate(template.idPlantilla)) {
                    if (schedule.idProgramacion <= 0L) continue
                    if (!alarmService.cancel(schedule.idProgramacion).succeeded()) {
                        return CancelAuthenticatedUserScheduleAlarmsResult.Failure()
                    }
                }
            }
            CancelAuthenticatedUserScheduleAlarmsResult.Cancelled
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            CancelAuthenticatedUserScheduleAlarmsResult.Failure(exception)
        }
    }

    private fun ScheduleAlarmCancelResult.succeeded(): Boolean = when (this) {
        ScheduleAlarmCancelResult.Cancelled -> true
        ScheduleAlarmCancelResult.InvalidScheduleId,
        is ScheduleAlarmCancelResult.Failure,
        -> false
    }
}
