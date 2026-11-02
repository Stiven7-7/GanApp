package com.proyecto.ganapp.domain.repository

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import kotlinx.coroutines.flow.Flow

interface NotificationTemplateRepository {
    fun getTemplatesByUser(userId: Long): Flow<List<PlantillaNotificacion>>
    suspend fun getTemplateById(templateId: Long): PlantillaNotificacion?
    suspend fun insertTemplate(template: PlantillaNotificacion): Long
    suspend fun updateTemplate(template: PlantillaNotificacion): Boolean
    suspend fun deleteTemplate(templateId: Long)
    suspend fun hasSchedules(templateId: Long): Boolean
}
