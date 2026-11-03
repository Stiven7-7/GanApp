package com.proyecto.ganapp.data.repository.impl

import com.proyecto.ganapp.data.local.dao.PlantillaNotificacionDao
import com.proyecto.ganapp.data.local.dao.ProgramacionNotificacionDao
import com.proyecto.ganapp.data.mapper.toDomain
import com.proyecto.ganapp.data.mapper.toEntity
import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Preparado para v11; sin binding productivo. */
class NotificationTemplateRepositoryImpl @Inject constructor(
    private val templateDao: PlantillaNotificacionDao,
    private val scheduleDao: ProgramacionNotificacionDao,
) : NotificationTemplateRepository {
    override fun getTemplatesByUser(userId: Long): Flow<List<PlantillaNotificacion>> =
        templateDao.getByUser(userId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTemplateById(templateId: Long): PlantillaNotificacion? =
        templateDao.getById(templateId)?.toDomain()

    override suspend fun insertTemplate(template: PlantillaNotificacion): Long =
        templateDao.insert(template.toEntity())

    override suspend fun updateTemplate(template: PlantillaNotificacion): Boolean =
        templateDao.updateContent(template.idPlantilla, template.titulo, template.tipo.toPersistentString()) > 0

    override suspend fun deleteTemplate(templateId: Long) {
        templateDao.deleteById(templateId)
    }

    override suspend fun hasSchedules(templateId: Long): Boolean =
        scheduleDao.countByTemplate(templateId) > 0
}
