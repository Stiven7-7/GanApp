package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertSame
import org.junit.Assert.fail

internal fun templateFixture(owner: Long = 7L, id: Long = 11L) =
    PlantillaNotificacion(id, "Vacuna", TipoNotificacion.VACUNACION, owner)

internal class TemplateSessionProbe(userId: Long? = 7L) : SessionRepository {
    val user = MutableStateFlow(userId)
    var observationCalls = 0
    var failure: Exception? = null

    override fun observeAuthenticatedUserId(): Flow<Long?> {
        observationCalls++
        return failure?.let { failure -> flow { throw failure } } ?: user
    }

    override suspend fun saveAuthenticatedUserId(userId: Long) {
        user.value = userId
    }

    override suspend fun clearAuthenticatedUserId() {
        user.value = null
    }
}

internal class TemplateRepositoryProbe : NotificationTemplateRepository {
    val calls = mutableListOf<String>()
    val queriedUsers = mutableListOf<Long>()
    val inserted = mutableListOf<PlantillaNotificacion>()
    val updated = mutableListOf<PlantillaNotificacion>()
    val deleted = mutableListOf<Long>()
    val observed = mutableMapOf<Long, MutableStateFlow<List<PlantillaNotificacion>>>()
    var current: PlantillaNotificacion? = templateFixture()
    var templates: List<PlantillaNotificacion> = emptyList()
    var insertId = 21L
    var updateResult = true
    var inUse = false
    var failingMethod: String? = null
    var failure: Exception = IllegalStateException("Repository probe failure")

    private fun record(method: String) {
        calls += method
        if (failingMethod == method) throw failure
    }

    override fun getTemplatesByUser(userId: Long): Flow<List<PlantillaNotificacion>> {
        record("list")
        queriedUsers += userId
        return observed[userId] ?: flowOf(templates)
    }

    override suspend fun getTemplateById(templateId: Long): PlantillaNotificacion? {
        record("get:$templateId")
        return current
    }

    override suspend fun insertTemplate(template: PlantillaNotificacion): Long {
        record("insert")
        inserted += template
        return insertId
    }

    override suspend fun updateTemplate(template: PlantillaNotificacion): Boolean {
        record("update")
        updated += template
        return updateResult
    }

    override suspend fun deleteTemplate(templateId: Long) {
        record("delete:$templateId")
        deleted += templateId
    }

    override suspend fun hasSchedules(templateId: Long): Boolean {
        record("has:$templateId")
        return inUse
    }
}

internal suspend fun assertTemplateCancellation(
    expected: CancellationException,
    block: suspend () -> Unit,
) {
    try {
        block()
        fail("CancellationException must propagate")
    } catch (actual: CancellationException) {
        assertSame(expected, actual)
    }
}
