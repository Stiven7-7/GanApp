package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.service.LegacyNotificationAlarmCanceller
import com.proyecto.ganapp.domain.service.NotificationScheduleAlarmService
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletedScheduleReconcileTest {
    @Test
    fun reconciliacionPosteriorNoRearmaLaProgramacionEliminada() = runTest {
        val zone = ZoneId.of("America/Bogota")
        val day = LocalDate.of(2026, 11, 15)
        val now = day.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val rows = mutableMapOf(
            77L to schedule(77L, day),
            78L to schedule(78L, day),
        )
        val scheduled = mutableListOf<Long>()
        val repository = object : NotificationScheduleRepository {
            override suspend fun insertSchedule(schedule: ProgramacionNotificacion) = 0L
            override suspend fun getScheduleById(scheduleId: Long) = rows[scheduleId]
            override suspend fun updateSchedule(schedule: ProgramacionNotificacion) = false
            override suspend fun deleteSchedule(scheduleId: Long) {
                rows.remove(scheduleId)
            }
            override suspend fun getSchedulesByAnimal(animalId: Long) = rows.values.filter { it.idAnimal == animalId }
            override suspend fun getSchedulesByTemplate(templateId: Long) =
                rows.values.filter { it.idPlantilla == templateId }
        }
        val templates = object : NotificationTemplateRepository {
            private val template = PlantillaNotificacion(5L, "Vacuna", TipoNotificacion.VACUNACION, 7L)
            override fun getTemplatesByUser(userId: Long): Flow<List<PlantillaNotificacion>> = flowOf(listOf(template))
            override suspend fun getTemplateById(templateId: Long) = template.takeIf { it.idPlantilla == templateId }
            override suspend fun insertTemplate(template: PlantillaNotificacion) = 0L
            override suspend fun updateTemplate(template: PlantillaNotificacion) = false
            override suspend fun deleteTemplate(templateId: Long) = Unit
            override suspend fun hasSchedules(templateId: Long) = false
        }
        val alarms = object : NotificationScheduleAlarmService {
            override fun schedule(scheduleId: Long, triggerAtMillis: Long): ScheduleAlarmResult {
                scheduled += scheduleId
                return ScheduleAlarmResult.Scheduled
            }
            override fun cancel(scheduleId: Long) = ScheduleAlarmCancelResult.Cancelled
        }
        val session = TemplateSessionProbe(7L)
        val deleted = DeleteNotificationScheduleUseCase(
            repository,
            templates,
            object : com.proyecto.ganapp.domain.repository.AnimalRepository {
                override fun getAllAnimals(userId: Long) = flowOf(emptyList<com.proyecto.ganapp.domain.model.Animal>())
                override fun getAnimalsByUser(userId: Long) = flowOf(emptyList<com.proyecto.ganapp.domain.model.Animal>())
                override fun getFavoriteAnimalsByUser(userId: Long) = flowOf(emptyList<com.proyecto.ganapp.domain.model.Animal>())
                override suspend fun getAnimalById(id: Long) = com.proyecto.ganapp.domain.model.Animal(
                    idAnimal = 9L,
                    especie = com.proyecto.ganapp.domain.model.EspecieAnimal.VACA,
                    nombre = "Mancha",
                    raza = "Holstein",
                    edad = null,
                    peso = null,
                    color = null,
                    fotoUri = null,
                    idUsuario = 7L,
                )
                override suspend fun insertAnimal(animal: com.proyecto.ganapp.domain.model.Animal) = 0L
                override suspend fun updateAnimal(animal: com.proyecto.ganapp.domain.model.Animal) = false
                override suspend fun deleteAnimal(id: Long) = Unit
            },
            session,
            alarms,
        )(77L)
        assertEquals(DeleteNotificationScheduleResult.Success, deleted)
        scheduled.clear()
        val reconciled = ReconcileNotificationScheduleAlarmsUseCase(
            session,
            templates,
            repository,
            alarms,
            LegacyNotificationAlarmCanceller { ScheduleAlarmCancelResult.Cancelled },
        )(now, zone)
        val result = reconciled as ReconcileNotificationScheduleAlarmsResult.Reconciled
        assertEquals(1, result.scheduled)
        assertEquals(listOf(78L), scheduled)
        assertFalse(scheduled.contains(77L))
        assertTrue(rows.containsKey(78L))
    }

    private fun schedule(id: Long, day: LocalDate) = ProgramacionNotificacion(
        idProgramacion = id,
        idPlantilla = 5L,
        idAnimal = 9L,
        fechaInicio = day,
        fechaFin = day.plusDays(5),
        hora = LocalTime.of(9, 0),
        recurrencia = RecurrenciaNotificacion.DIARIA,
    )
}
