package com.proyecto.ganapp.domain.repository

import com.proyecto.ganapp.domain.model.ProgramacionNotificacion

interface NotificationScheduleRepository {
    suspend fun insertSchedule(schedule: ProgramacionNotificacion): Long
    suspend fun getScheduleById(scheduleId: Long): ProgramacionNotificacion?
    suspend fun updateSchedule(schedule: ProgramacionNotificacion): Boolean
    suspend fun deleteSchedule(scheduleId: Long)
    suspend fun getSchedulesByAnimal(animalId: Long): List<ProgramacionNotificacion>
    suspend fun getSchedulesByTemplate(templateId: Long): List<ProgramacionNotificacion>
}
