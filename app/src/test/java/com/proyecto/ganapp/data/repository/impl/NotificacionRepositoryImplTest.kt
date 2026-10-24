package com.proyecto.ganapp.data.repository.impl

import com.proyecto.ganapp.data.local.dao.NotificacionDao
import com.proyecto.ganapp.data.local.entity.NotificacionEntity
import com.proyecto.ganapp.domain.model.Notificacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificacionRepositoryImplTest {

    @Test
    fun insertNotification_propagaExactamenteElIdDelDao() = runTest {
        val dao = FakeNotificacionDao(insertedId = 42L)
        val repository = NotificacionRepositoryImpl(dao)

        val id = repository.insertNotification(
            Notificacion(
                idNotificacion = 0L,
                nombre = "Vacuna",
                tipo = "Vacunación",
                fechaInicio = 10L,
                fechaFin = 10L,
                seRepite = "NO",
                hora = "08:30",
                idUsuario = 3L,
            ),
        )

        assertEquals(42L, id)
        assertEquals(0L, dao.lastInserted?.idNotificacion)
        assertEquals("Vacuna", dao.lastInserted?.nombre)
    }
}

private class FakeNotificacionDao(
    private val insertedId: Long,
) : NotificacionDao {
    var lastInserted: NotificacionEntity? = null

    override suspend fun insert(notificacion: NotificacionEntity): Long {
        lastInserted = notificacion
        return insertedId
    }

    override suspend fun update(notificacion: NotificacionEntity) = Unit

    override suspend fun deleteById(id: Long) = Unit

    override suspend fun getById(id: Long): NotificacionEntity? = null

    override fun getAll(): Flow<List<NotificacionEntity>> = emptyFlow()

    override fun getActiveNotifications(fechaActual: String): Flow<List<NotificacionEntity>> = emptyFlow()
}
