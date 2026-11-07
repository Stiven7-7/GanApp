package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import java.time.ZoneId
import javax.inject.Inject

class CreateAnimalNotificationScheduleUseCase @Inject constructor(
    private val repository: NotificationScheduleRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val animalRepository: AnimalRepository,
    private val sessionRepository: SessionRepository,
    private val alarmService: NotificationScheduleAlarmService,
) {
    suspend operator fun invoke(
        request: CreateAnimalNotificationScheduleRequest,
        now: Long,
        zoneId: ZoneId,
    ): CreateAnimalNotificationScheduleResult {
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) return CreateAnimalNotificationScheduleResult.NoAuthenticatedUser
            val validation = NotificationScheduleValidator.validate(request, now, zoneId)
            if (validation is ScheduleValidationResult.Invalid) {
                return CreateAnimalNotificationScheduleResult.InvalidInput(validation.error)
            }
            val template = templateRepository.getTemplateById(request.templateId)
                ?: return CreateAnimalNotificationScheduleResult.TemplateNotFound
            if (template.idUsuario != userId) return CreateAnimalNotificationScheduleResult.OwnershipMismatch
            val animal = animalRepository.getAnimalById(request.animalId)
                ?: return CreateAnimalNotificationScheduleResult.AnimalNotFound
            if (animal.idUsuario != userId) return CreateAnimalNotificationScheduleResult.OwnershipMismatch
            val schedule = ProgramacionNotificacion(
                idPlantilla = request.templateId,
                idAnimal = request.animalId,
                fechaInicio = request.fechaInicio,
                fechaFin = request.fechaFin,
                hora = request.hora,
                recurrencia = request.recurrencia,
            )
            val id = repository.insertSchedule(schedule)
            if (id <= 0L) {
                return CreateAnimalNotificationScheduleResult.Failure(
                    IllegalStateException("Invalid inserted schedule ID: $id"),
                )
            }
            // La fila ya existe: todo fallo de alarma conserva el ID real y no revierte.
            try {
                when (val alarm = alarmService.schedule(
                    id, (validation as ScheduleValidationResult.Valid).nextOccurrenceAtMillis,
                )) {
                    ScheduleAlarmResult.Scheduled -> CreateAnimalNotificationScheduleResult.Success(id)
                    is ScheduleAlarmResult.Failure ->
                        CreateAnimalNotificationScheduleResult.SchedulingFailure(id, alarm.cause)
                    ScheduleAlarmResult.InvalidScheduleId,
                    ScheduleAlarmResult.InvalidTriggerTime,
                    -> CreateAnimalNotificationScheduleResult.SchedulingFailure(id)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                CreateAnimalNotificationScheduleResult.SchedulingFailure(id, exception)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            CreateAnimalNotificationScheduleResult.Failure(exception)
        }
    }
}
