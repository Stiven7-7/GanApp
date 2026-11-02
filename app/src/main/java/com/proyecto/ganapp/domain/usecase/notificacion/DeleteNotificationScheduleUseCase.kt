package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult

class DeleteNotificationScheduleUseCase(
    private val repository: NotificationScheduleRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val animalRepository: AnimalRepository,
    private val sessionRepository: SessionRepository,
    private val alarmService: NotificationScheduleAlarmService,
) {
    suspend operator fun invoke(scheduleId: Long): DeleteNotificationScheduleResult {
        if (scheduleId <= 0L) return DeleteNotificationScheduleResult.InvalidScheduleId
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) return DeleteNotificationScheduleResult.NoAuthenticatedUser
            val schedule = repository.getScheduleById(scheduleId)
                ?: return DeleteNotificationScheduleResult.ScheduleNotFound
            val template = templateRepository.getTemplateById(schedule.idPlantilla)
                ?: return DeleteNotificationScheduleResult.TemplateNotFound
            if (template.idUsuario != userId) return DeleteNotificationScheduleResult.OwnershipMismatch
            val animalId = schedule.idAnimal
            if (animalId != null) {
                val animal = animalRepository.getAnimalById(animalId)
                    ?: return DeleteNotificationScheduleResult.AnimalNotFound
                if (animal.idUsuario != userId) return DeleteNotificationScheduleResult.OwnershipMismatch
            }
            val cancellation = try {
                alarmService.cancel(schedule.idProgramacion)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                return DeleteNotificationScheduleResult.AlarmCancellationFailed(exception)
            }
            when (cancellation) {
                is ScheduleAlarmCancelResult.Failure ->
                    return DeleteNotificationScheduleResult.AlarmCancellationFailed(cancellation.cause)
                ScheduleAlarmCancelResult.InvalidScheduleId ->
                    return DeleteNotificationScheduleResult.AlarmCancellationFailed()
                ScheduleAlarmCancelResult.Cancelled -> Unit
            }
            try {
                repository.deleteSchedule(schedule.idProgramacion)
                DeleteNotificationScheduleResult.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                DeleteNotificationScheduleResult.PersistenceFailure(exception)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            DeleteNotificationScheduleResult.Failure(exception)
        }
    }
}
