package com.proyecto.ganapp.data.repository

import com.proyecto.ganapp.data.local.dao.PlantillaNotificacionDao
import com.proyecto.ganapp.data.local.dao.ProgramacionNotificacionDao
import com.proyecto.ganapp.data.local.entity.PlantillaNotificacionEntity
import com.proyecto.ganapp.data.local.entity.ProgramacionNotificacionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class TemplateDaoProbe : PlantillaNotificacionDao {
    val calls = mutableListOf<Pair<String, Long>>()
    var emissions: Flow<List<PlantillaNotificacionEntity>> = flowOf(emptyList())
    var entity: PlantillaNotificacionEntity? = null
    var written: PlantillaNotificacionEntity? = null
    var insertedId = 42L
    var updatedRows = 1

    override fun getByUser(userId: Long): Flow<List<PlantillaNotificacionEntity>> {
        calls += "user" to userId
        return emissions
    }

    override suspend fun getById(id: Long): PlantillaNotificacionEntity? {
        calls += "get" to id
        return entity
    }

    override suspend fun insert(template: PlantillaNotificacionEntity): Long {
        calls += "insert" to template.idPlantilla
        written = template
        return insertedId
    }

    var updatedContent: Triple<Long, String, String>? = null

    override suspend fun updateContent(idPlantilla: Long, titulo: String, tipo: String): Int {
        calls += "updateContent" to idPlantilla
        updatedContent = Triple(idPlantilla, titulo, tipo)
        val existing = entity
        if (updatedRows > 0 && existing?.idPlantilla == idPlantilla) {
            entity = existing.copy(titulo = titulo, tipo = tipo)
        }
        return updatedRows
    }

    override suspend fun deleteById(id: Long) {
        calls += "delete" to id
    }
}

internal class ScheduleDaoProbe : ProgramacionNotificacionDao {
    val calls = mutableListOf<Pair<String, Long>>()
    var entity: ProgramacionNotificacionEntity? = null
    var entities = emptyList<ProgramacionNotificacionEntity>()
    var written: ProgramacionNotificacionEntity? = null
    var insertedId = 77L
    var updatedRows = 1
    var count = 0L

    override suspend fun insert(schedule: ProgramacionNotificacionEntity): Long {
        calls += "insert" to schedule.idProgramacion
        written = schedule
        return insertedId
    }

    override suspend fun getById(id: Long): ProgramacionNotificacionEntity? {
        calls += "get" to id
        return entity
    }

    override suspend fun update(schedule: ProgramacionNotificacionEntity): Int {
        calls += "update" to schedule.idProgramacion
        written = schedule
        return updatedRows
    }

    override suspend fun deleteById(id: Long) {
        calls += "delete" to id
    }

    override suspend fun getByAnimal(animalId: Long): List<ProgramacionNotificacionEntity> {
        calls += "animal" to animalId
        return entities
    }

    override suspend fun getByTemplate(templateId: Long): List<ProgramacionNotificacionEntity> {
        calls += "template" to templateId
        return entities
    }

    override suspend fun countByTemplate(templateId: Long): Long {
        calls += "count" to templateId
        return count
    }
}
