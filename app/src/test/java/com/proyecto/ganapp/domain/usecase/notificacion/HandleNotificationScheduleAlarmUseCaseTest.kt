package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
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
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleNotificationScheduleAlarmUseCaseTest {
    private val zone: ZoneId = ZoneId.of("America/Bogota")
    private val firedAt: Long = 1_000L

    @Test
    fun muestraTituloYTipoActualesYProgramaSoloLaSiguientePorIdProgramacion() = runTest {
        val alarms = AlarmProbe()
        val useCase = useCase(alarms, templateTitle = "Refuerzo", scheduleId = 15L)
        val prepared = useCase.prepare(15L, firedAt, zone)
        useCase.armNext(15L, firedAt, zone)

        val display = prepared as HandleNotificationScheduleAlarmResult.Display
        assertEquals("Refuerzo", display.title)
        assertEquals("Vacunación", display.message)
        assertEquals(listOf(15L), alarms.scheduledIds)
        assertTrue(alarms.scheduledIds.none { it == 10L })
    }

    @Test
    fun sinProgramacionNoMuestraNiUsaIdDePlantilla() = runTest {
        val alarms = AlarmProbe()
        val useCase = useCase(alarms, templateTitle = "Vacuna", scheduleId = null)
        assertEquals(HandleNotificationScheduleAlarmResult.Ignored, useCase.prepare(15L, firedAt, zone))
        useCase.armNext(15L, firedAt, zone)
        assertTrue(alarms.scheduledIds.isEmpty())
    }

    private fun useCase(alarms: AlarmProbe, templateTitle: String, scheduleId: Long?): HandleNotificationScheduleAlarmUseCase {
        val schedule = scheduleId?.let {
            ProgramacionNotificacion(
                idProgramacion = it,
                idPlantilla = 10L,
                idAnimal = 7L,
                fechaInicio = LocalDate.of(2026, 6, 15),
                fechaFin = LocalDate.of(2026, 6, 16),
                hora = LocalTime.of(8, 0),
                recurrencia = RecurrenciaNotificacion.DIARIA,
            )
        }
        return HandleNotificationScheduleAlarmUseCase(
            scheduleRepository = object : NotificationScheduleRepository {
                override suspend fun insertSchedule(schedule: ProgramacionNotificacion) = 0L
                override suspend fun getScheduleById(id: Long) = schedule?.takeIf { it.idProgramacion == id }
                override suspend fun updateSchedule(schedule: ProgramacionNotificacion) = false
                override suspend fun deleteSchedule(scheduleId: Long) = Unit
                override suspend fun getSchedulesByAnimal(animalId: Long) = emptyList<ProgramacionNotificacion>()
                override suspend fun getSchedulesByTemplate(templateId: Long) = emptyList<ProgramacionNotificacion>()
            },
            templateRepository = object : NotificationTemplateRepository {
                override fun getTemplatesByUser(userId: Long): Flow<List<PlantillaNotificacion>> = flowOf(emptyList())
                override suspend fun getTemplateById(templateId: Long) =
                    PlantillaNotificacion(templateId, templateTitle, TipoNotificacion.VACUNACION, 1L)
                override suspend fun insertTemplate(template: PlantillaNotificacion) = 0L
                override suspend fun updateTemplate(template: PlantillaNotificacion) = false
                override suspend fun deleteTemplate(templateId: Long) = Unit
                override suspend fun hasSchedules(templateId: Long) = false
            },
            alarmService = alarms,
        )
    }

    private class AlarmProbe : NotificationScheduleAlarmService {
        val scheduledIds = mutableListOf<Long>()
        override fun schedule(scheduleId: Long, triggerAtMillis: Long): ScheduleAlarmResult {
            scheduledIds += scheduleId
            return ScheduleAlarmResult.Scheduled
        }
        override fun cancel(scheduleId: Long) = ScheduleAlarmCancelResult.Cancelled
    }
}
