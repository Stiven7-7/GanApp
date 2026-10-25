package com.proyecto.ganapp.data.repository.impl

import com.proyecto.ganapp.data.local.dao.NotificacionxAnimalDao
import com.proyecto.ganapp.data.local.entity.NotificacionxAnimalEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificacionxAnimalRepositoryImplTest {

    @Test
    fun getNotificationIdsOfAnimal_devuelveSoloLosIdsDeNotificacion() = runTest {
        val dao = LinkIdProbeDao(
            byAnimal = listOf(
                NotificacionxAnimalEntity(idAnimal = 4L, idNotificacion = 9L),
                NotificacionxAnimalEntity(idAnimal = 4L, idNotificacion = 11L),
            ),
        )
        val repository = NotificacionxAnimalRepositoryImpl(dao)

        val ids = repository.getNotificationIdsOfAnimal(4L)

        assertEquals(listOf(9L, 11L), ids)
    }

    @Test
    fun getAnimalIdsWithNotification_devuelveSoloLosIdsDeAnimal() = runTest {
        val dao = LinkIdProbeDao(
            byNotification = listOf(
                NotificacionxAnimalEntity(idAnimal = 4L, idNotificacion = 9L),
                NotificacionxAnimalEntity(idAnimal = 8L, idNotificacion = 9L),
            ),
        )
        val repository = NotificacionxAnimalRepositoryImpl(dao)

        val ids = repository.getAnimalIdsWithNotification(9L)

        assertEquals(listOf(4L, 8L), ids)
    }
}

private class LinkIdProbeDao(
    private val byAnimal: List<NotificacionxAnimalEntity> = emptyList(),
    private val byNotification: List<NotificacionxAnimalEntity> = emptyList(),
) : NotificacionxAnimalDao {

    override suspend fun insert(link: NotificacionxAnimalEntity) = Unit

    override suspend fun delete(idAnimal: Long, idNotificacion: Long) = Unit

    override suspend fun getNotificacionesByAnimal(idAnimal: Long): List<NotificacionxAnimalEntity> {
        return byAnimal
    }

    override suspend fun getAnimalesByNotificacion(
        idNotificacion: Long,
    ): List<NotificacionxAnimalEntity> = byNotification
}
