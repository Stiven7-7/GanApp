package com.proyecto.ganapp.data.repository.impl

import com.proyecto.ganapp.data.local.dao.ProgramacionNotificacionDao
import com.proyecto.ganapp.data.mapper.toDomain
import com.proyecto.ganapp.data.mapper.toEntity
import com.proyecto.ganapp.domain.model.ProgramacionNotificacion
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import javax.inject.Inject

/** Preparado para v11; sin binding productivo. */
class NotificationScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ProgramacionNotificacionDao,
) : NotificationScheduleRepository {
    override suspend fun insertSchedule(schedule: ProgramacionNotificacion): Long =
        scheduleDao.insert(schedule.toEntity())

    override suspend fun getScheduleById(scheduleId: Long): ProgramacionNotificacion? =
        scheduleDao.getById(scheduleId)?.toDomain()

    override suspend fun updateSchedule(schedule: ProgramacionNotificacion): Boolean =
        scheduleDao.update(schedule.toEntity()) > 0

    override suspend fun deleteSchedule(scheduleId: Long) {
        scheduleDao.deleteById(scheduleId)
    }

    override suspend fun getSchedulesByAnimal(animalId: Long): List<ProgramacionNotificacion> =
        scheduleDao.getByAnimal(animalId).map { it.toDomain() }

    override suspend fun getSchedulesByTemplate(templateId: Long): List<ProgramacionNotificacion> =
        scheduleDao.getByTemplate(templateId).map { it.toDomain() }
}
