package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import java.time.ZoneId

class UpdateNotificationScheduleUseCase(
    private val repository: NotificationScheduleRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val animalRepository: AnimalRepository,
    private val sessionRepository: SessionRepository,
    private val alarmService: NotificationScheduleAlarmService,
) {
    suspend operator fun invoke(
        request: UpdateNotificationScheduleRequest,
        now: Long,
        zoneId: ZoneId,
    ): UpdateNotificationScheduleResult {
        val validation = NotificationScheduleValidator.validate(request, now, zoneId)
        if (validation is ScheduleValidationResult.Invalid) {
            return UpdateNotificationScheduleResult.InvalidInput(validation.error)
        }
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) return UpdateNotificationScheduleResult.NoAuthenticatedUser
            val original = repository.getScheduleById(request.scheduleId)
                ?: return UpdateNotificationScheduleResult.ScheduleNotFound
            val template = templateRepository.getTemplateById(original.idPlantilla)
                ?: return UpdateNotificationScheduleResult.TemplateNotFound
            if (template.idUsuario != userId) return UpdateNotificationScheduleResult.OwnershipMismatch
            val animalId = original.idAnimal
            if (animalId != null) {
                val animal = animalRepository.getAnimalById(animalId)
                    ?: return UpdateNotificationScheduleResult.AnimalNotFound
                if (animal.idUsuario != userId) return UpdateNotificationScheduleResult.OwnershipMismatch
            }
            val updated = original.copy(
                fechaInicio = request.fechaInicio,
                fechaFin = request.fechaFin,
                hora = request.hora,
                recurrencia = request.recurrencia,
            )
            if (!repository.updateSchedule(updated)) {
                return UpdateNotificationScheduleResult.Failure(
                    IllegalStateException("Schedule update was not applied"),
                )
            }
            try {
                when (val alarm = alarmService.schedule(
                    original.idProgramacion,
                    (validation as ScheduleValidationResult.Valid).nextOccurrenceAtMillis,
                )) {
                    ScheduleAlarmResult.Scheduled -> UpdateNotificationScheduleResult.Success
                    is ScheduleAlarmResult.Failure ->
                        UpdateNotificationScheduleResult.SchedulingFailure(original.idProgramacion, alarm.cause)
                    ScheduleAlarmResult.InvalidScheduleId,
                    ScheduleAlarmResult.InvalidTriggerTime,
                    -> UpdateNotificationScheduleResult.SchedulingFailure(original.idProgramacion)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                UpdateNotificationScheduleResult.SchedulingFailure(original.idProgramacion, exception)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            UpdateNotificationScheduleResult.Failure(exception)
        }
    }
}
