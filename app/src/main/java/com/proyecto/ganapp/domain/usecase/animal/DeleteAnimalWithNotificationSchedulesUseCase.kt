package com.proyecto.ganapp.domain.usecase.animal

import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.AnimalScheduleDeletionRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/** Coordinación preparada para el cutover futuro; no conectada a UI ni DI productiva. */
class DeleteAnimalWithNotificationSchedulesUseCase(
    private val sessionRepository: SessionRepository,
    private val animalRepository: AnimalRepository,
    private val scheduleRepository: NotificationScheduleRepository,
    private val alarmService: NotificationScheduleAlarmService,
    private val deletionRepository: AnimalScheduleDeletionRepository,
) {
    suspend operator fun invoke(animalId: Long): DeleteAnimalWithNotificationSchedulesResult {
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) {
                return DeleteAnimalWithNotificationSchedulesResult.NoAuthenticatedUser
            }
            if (animalId <= 0L) return DeleteAnimalWithNotificationSchedulesResult.InvalidAnimalId
            val animal = animalRepository.getAnimalById(animalId)
                ?: return DeleteAnimalWithNotificationSchedulesResult.AnimalNotFound
            if (animal.idUsuario != userId) {
                return DeleteAnimalWithNotificationSchedulesResult.OwnershipMismatch
            }
            val schedules = scheduleRepository.getSchedulesByAnimal(animalId)
            for (schedule in schedules) {
                val cancellation = try {
                    alarmService.cancel(schedule.idProgramacion)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    return DeleteAnimalWithNotificationSchedulesResult.AlarmCancellationFailed(
                        schedule.idProgramacion, exception,
                    )
                }
                when (cancellation) {
                    ScheduleAlarmCancelResult.Cancelled -> Unit
                    ScheduleAlarmCancelResult.InvalidScheduleId ->
                        return DeleteAnimalWithNotificationSchedulesResult.AlarmCancellationFailed(
                            schedule.idProgramacion,
                        )
                    is ScheduleAlarmCancelResult.Failure ->
                        return DeleteAnimalWithNotificationSchedulesResult.AlarmCancellationFailed(
                            schedule.idProgramacion, cancellation.cause,
                        )
                }
            }
            try {
                deletionRepository.deleteAnimalAndSchedules(animalId)
                DeleteAnimalWithNotificationSchedulesResult.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                DeleteAnimalWithNotificationSchedulesResult.PersistenceFailure(exception)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            DeleteAnimalWithNotificationSchedulesResult.Failure(exception)
        }
    }
}
