package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

internal class ScheduleTestFixture {
    val zone: ZoneId = ZoneId.of("America/Bogota")
    val day: LocalDate = LocalDate.of(2026, 11, 15)
    val now: Long = at(day, LocalTime.of(8, 0))
    val session = TemplateSessionProbe()
    val events = mutableListOf<String>()
    val failures = mutableMapOf<String, Exception>()
    val templates = mutableMapOf(5L to PlantillaNotificacion(
        idPlantilla = 5L, titulo = "Vacuna", tipo = TipoNotificacion.VACUNACION, idUsuario = 7L,
    ))
    val animals = mutableMapOf(9L to Animal(
        idAnimal = 9L, especie = EspecieAnimal.VACA, nombre = "Mancha", raza = "Holstein",
        edad = null, peso = null, color = null, fotoUri = null, idUsuario = 7L,
    ))
    val schedules = mutableMapOf(77L to schedule())
    val inserted = mutableListOf<ProgramacionNotificacion>()
    val updated = mutableListOf<ProgramacionNotificacion>()
    val deleted = mutableListOf<Long>()
    val scheduled = mutableListOf<Pair<Long, Long>>()
    val cancelled = mutableListOf<Long>()
    val activeAlarms = mutableMapOf<Long, Long>()
    var nextInsertId = 101L
    var updateResult = true
    var listOverride: List<ProgramacionNotificacion>? = null
    var alarmResult: ScheduleAlarmResult = ScheduleAlarmResult.Scheduled
    var cancelResult: ScheduleAlarmCancelResult = ScheduleAlarmCancelResult.Cancelled

    private fun record(event: String) {
        events += event
        failures[event]?.let { throw it }
    }

    val repository = object : NotificationScheduleRepository {
        override suspend fun insertSchedule(schedule: ProgramacionNotificacion): Long {
            record("insert")
            inserted += schedule
            val id = nextInsertId++
            if (id > 0L) schedules[id] = schedule.copy(idProgramacion = id)
            return id
        }
        override suspend fun getScheduleById(scheduleId: Long): ProgramacionNotificacion? {
            record("get:$scheduleId")
            return schedules[scheduleId]
        }
        override suspend fun updateSchedule(schedule: ProgramacionNotificacion): Boolean {
            record("update")
            updated += schedule
            if (updateResult) schedules[schedule.idProgramacion] = schedule
            return updateResult
        }
        override suspend fun deleteSchedule(scheduleId: Long) {
            record("delete:$scheduleId")
            schedules.remove(scheduleId)
            deleted += scheduleId
        }
        override suspend fun getSchedulesByAnimal(animalId: Long): List<ProgramacionNotificacion> {
            record("listAnimal:$animalId")
            return listOverride ?: schedules.values.filter { it.idAnimal == animalId }
        }
        override suspend fun getSchedulesByTemplate(templateId: Long): List<ProgramacionNotificacion> {
            record("listTemplate:$templateId")
            return schedules.values.filter { it.idPlantilla == templateId }
        }
    }

    val templateRepository = object : NotificationTemplateRepository {
        override suspend fun getTemplateById(templateId: Long): PlantillaNotificacion? {
            record("template:$templateId")
            return templates[templateId]
        }
        override fun getTemplatesByUser(userId: Long): Flow<List<PlantillaNotificacion>> = unexpected()
        override suspend fun insertTemplate(template: PlantillaNotificacion): Long = unexpected()
        override suspend fun updateTemplate(template: PlantillaNotificacion): Boolean = unexpected()
        override suspend fun deleteTemplate(templateId: Long): Unit = unexpected()
        override suspend fun hasSchedules(templateId: Long): Boolean = unexpected()
    }

    val animalRepository = object : AnimalRepository {
        override suspend fun getAnimalById(id: Long): Animal? {
            record("animal:$id")
            return animals[id]
        }
        override fun getAllAnimals(userId: Long): Flow<List<Animal>> = unexpected()
        override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = unexpected()
        override fun getFavoriteAnimalsByUser(userId: Long): Flow<List<Animal>> = unexpected()
        override suspend fun insertAnimal(animal: Animal): Long = unexpected()
        override suspend fun updateAnimal(animal: Animal): Boolean = unexpected()
        override suspend fun deleteAnimal(id: Long): Unit = unexpected()
    }

    val alarms = object : NotificationScheduleAlarmService {
        override fun schedule(scheduleId: Long, triggerAtMillis: Long): ScheduleAlarmResult {
            record("schedule:$scheduleId")
            scheduled += scheduleId to triggerAtMillis
            if (alarmResult == ScheduleAlarmResult.Scheduled) activeAlarms[scheduleId] = triggerAtMillis
            return alarmResult
        }
        override fun cancel(scheduleId: Long): ScheduleAlarmCancelResult {
            record("cancel:$scheduleId")
            cancelled += scheduleId
            if (cancelResult == ScheduleAlarmCancelResult.Cancelled) activeAlarms.remove(scheduleId)
            return cancelResult
        }
    }

    fun schedule(id: Long = 77L, animalId: Long? = 9L, templateId: Long = 5L) =
        ProgramacionNotificacion(
            idProgramacion = id, idPlantilla = templateId, idAnimal = animalId,
            fechaInicio = day, fechaFin = day, hora = LocalTime.of(7, 0),
            recurrencia = RecurrenciaNotificacion.NO,
        )

    fun createRequest(time: LocalTime = LocalTime.of(9, 0)) = CreateAnimalNotificationScheduleRequest(
        templateId = 5L, animalId = 9L, fechaInicio = day, fechaFin = day,
        hora = time, recurrencia = RecurrenciaNotificacion.NO,
    )

    fun updateRequest() = UpdateNotificationScheduleRequest(
        scheduleId = 77L, fechaInicio = day, fechaFin = day,
        hora = LocalTime.of(9, 0), recurrencia = RecurrenciaNotificacion.NO,
    )

    suspend fun create(request: CreateAnimalNotificationScheduleRequest = createRequest()) =
        CreateAnimalNotificationScheduleUseCase(repository, templateRepository, animalRepository, session, alarms)(
            request, now, zone,
        )

    suspend fun update(request: UpdateNotificationScheduleRequest = updateRequest()) =
        UpdateNotificationScheduleUseCase(repository, templateRepository, animalRepository, session, alarms)(
            request, now, zone,
        )

    suspend fun read(id: Long = 77L) =
        GetNotificationScheduleByIdUseCase(repository, templateRepository, animalRepository, session)(id)

    suspend fun list(animalId: Long = 9L) =
        GetAnimalNotificationSchedulesUseCase(repository, templateRepository, animalRepository, session)(animalId)

    suspend fun delete(id: Long = 77L) =
        DeleteNotificationScheduleUseCase(repository, templateRepository, animalRepository, session, alarms)(id)

    fun at(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun unexpected(): Nothing = error("Unexpected probe operation")
}
