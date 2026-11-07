package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.LegacyNotificationAlarmCanceller
import com.proyecto.ganapp.domain.service.NotificationRecurrenceEngine
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class ReconcileNotificationScheduleAlarmsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val scheduleRepository: NotificationScheduleRepository,
    private val alarmService: NotificationScheduleAlarmService,
    private val legacyCanceller: LegacyNotificationAlarmCanceller,
) {
    suspend operator fun invoke(
        now: Long,
        zoneId: ZoneId,
    ): ReconcileNotificationScheduleAlarmsResult {
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) {
                return ReconcileNotificationScheduleAlarmsResult.NoAuthenticatedUser
            }
            val templates = templateRepository.getTemplatesByUser(userId).first()
                .filter { it.idUsuario == userId }
            var scheduled = 0
            var failed = 0
            for (template in templates) {
                if (template.idPlantilla <= 0L) continue
                val legacy = legacyCanceller.cancel(template.idPlantilla)
                if (!legacy.succeeded()) {
                    failed += 1
                    continue
                }
                val schedules = scheduleRepository.getSchedulesByTemplate(template.idPlantilla)
                for (schedule in schedules) {
                    if (schedule.idProgramacion <= 0L) continue
                    if (arm(schedule, now, zoneId)) scheduled += 1 else failed += 1
                }
            }
            ReconcileNotificationScheduleAlarmsResult.Reconciled(scheduled, failed)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            ReconcileNotificationScheduleAlarmsResult.Failure(exception)
        }
    }

    private fun arm(schedule: ProgramacionNotificacion, now: Long, zoneId: ZoneId): Boolean {
        val next = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = schedule.recurrencia,
            fechaInicio = schedule.fechaInicio.toEpochDay(),
            fechaFin = schedule.fechaFin.toEpochDay(),
            hora = schedule.hora.format(HOUR),
            afterMillis = now,
            zoneId = zoneId,
        )
        return if (next == null) {
            alarmService.cancel(schedule.idProgramacion).succeeded()
        } else {
            when (val result = alarmService.schedule(schedule.idProgramacion, next)) {
                ScheduleAlarmResult.Scheduled -> true
                ScheduleAlarmResult.InvalidScheduleId,
                ScheduleAlarmResult.InvalidTriggerTime,
                is ScheduleAlarmResult.Failure,
                -> false
            }
        }
    }

    private fun ScheduleAlarmCancelResult.succeeded(): Boolean = when (this) {
        ScheduleAlarmCancelResult.Cancelled -> true
        ScheduleAlarmCancelResult.InvalidScheduleId,
        is ScheduleAlarmCancelResult.Failure,
        -> false
    }

    private companion object {
        val HOUR: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
