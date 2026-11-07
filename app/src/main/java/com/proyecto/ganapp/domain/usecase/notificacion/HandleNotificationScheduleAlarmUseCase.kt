package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.service.NotificationRecurrenceEngine
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class HandleNotificationScheduleAlarmUseCase @Inject constructor(
    private val scheduleRepository: NotificationScheduleRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val alarmService: NotificationScheduleAlarmService,
) {
    suspend fun prepare(
        scheduleId: Long,
        firedAtMillis: Long,
        zoneId: ZoneId,
    ): HandleNotificationScheduleAlarmResult {
        if (scheduleId <= 0L) return HandleNotificationScheduleAlarmResult.Ignored
        val schedule = try {
            scheduleRepository.getScheduleById(scheduleId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return HandleNotificationScheduleAlarmResult.Ignored
        } ?: return HandleNotificationScheduleAlarmResult.Ignored
        val template = try {
            templateRepository.getTemplateById(schedule.idPlantilla)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return HandleNotificationScheduleAlarmResult.Ignored
        } ?: return HandleNotificationScheduleAlarmResult.Ignored
        return HandleNotificationScheduleAlarmResult.Display(
            title = template.titulo,
            message = template.tipo.toPersistentString(),
        )
    }

    suspend fun armNext(scheduleId: Long, firedAtMillis: Long, zoneId: ZoneId) {
        if (scheduleId <= 0L) return
        val schedule = try {
            scheduleRepository.getScheduleById(scheduleId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return
        }
        if (schedule == null) return
        val templateExists = try {
            templateRepository.getTemplateById(schedule.idPlantilla) != null
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            false
        }
        if (!templateExists) {
            alarmService.cancel(scheduleId)
            return
        }
        val next = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = schedule.recurrencia,
            fechaInicio = schedule.fechaInicio.toEpochDay(),
            fechaFin = schedule.fechaFin.toEpochDay(),
            hora = schedule.hora.format(HOUR),
            afterMillis = firedAtMillis,
            zoneId = zoneId,
        )
        if (next == null) {
            alarmService.cancel(scheduleId)
        } else {
            alarmService.schedule(scheduleId, next)
        }
    }

    private companion object {
        val HOUR: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
