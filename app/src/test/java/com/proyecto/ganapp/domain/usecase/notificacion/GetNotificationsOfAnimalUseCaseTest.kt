package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.data.local.entity.NotificacionxAnimalEntity
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetNotificationsOfAnimalUseCaseTest {

    private lateinit var animals: AnimalReadProbeRepository
    private lateinit var notifications: NotificationReadProbeRepository
    private lateinit var links: LinkReadProbeRepository
    private lateinit var useCase: GetNotificationsOfAnimalUseCase

    @Before
    fun setUp() {
        animals = AnimalReadProbeRepository()
        notifications = NotificationReadProbeRepository()
        links = LinkReadProbeRepository()
        useCase = GetNotificationsOfAnimalUseCase(
            animalRepository = animals,
            linkRepository = links,
            notificationRepository = notifications,
        )
    }

    @Test
    fun animalInexistente_devuelveVacioSinConsultarRelaciones() = runTest {
        animals.animal = null

        val result = useCase(4L)

        assertTrue(result.isEmpty())
        assertEquals(0, links.calls)
    }

    @Test
    fun notificacionFaltante_seIgnora() = runTest {
        animals.animal = animal(ownerId = 7L)
        links.links = listOf(NotificacionxAnimalEntity(4L, 9L))
        notifications.byId = emptyMap()

        val result = useCase(4L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun mismaPropiedad_seIncluye() = runTest {
        val own = notification(id = 9L, ownerId = 7L)
        animals.animal = animal(ownerId = 7L)
        links.links = listOf(NotificacionxAnimalEntity(4L, 9L))
        notifications.byId = mapOf(9L to own)

        val result = useCase(4L)

        assertEquals(listOf(own), result)
    }

    @Test
    fun otraPropiedad_seExcluye() = runTest {
        animals.animal = animal(ownerId = 7L)
        links.links = listOf(NotificacionxAnimalEntity(4L, 9L))
        notifications.byId = mapOf(9L to notification(id = 9L, ownerId = 8L))

        val result = useCase(4L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun mezcla_devuelveSoloLasDelDuenoDelAnimal() = runTest {
        val own = notification(id = 9L, ownerId = 7L)
        animals.animal = animal(ownerId = 7L)
        links.links = listOf(
            NotificacionxAnimalEntity(4L, 9L),
            NotificacionxAnimalEntity(4L, 10L),
            NotificacionxAnimalEntity(4L, 11L),
        )
        notifications.byId = mapOf(
            9L to own,
            10L to notification(id = 10L, ownerId = 8L),
        )

        val result = useCase(4L)

        assertEquals(listOf(own), result)
    }

    private fun animal(ownerId: Long): Animal {
        return Animal(
            idAnimal = 4L,
            especie = EspecieAnimal.VACA,
            nombre = "Lola",
            raza = "Holstein",
            edad = null,
            peso = null,
            color = null,
            fotoUri = null,
            idUsuario = ownerId,
        )
    }

    private fun notification(id: Long, ownerId: Long): Notificacion {
        return Notificacion(
            idNotificacion = id,
            nombre = "Vacuna $id",
            tipo = "Vacunación",
            fechaInicio = 20_000L,
            fechaFin = 20_000L,
            seRepite = "NO",
            hora = "08:00",
            idUsuario = ownerId,
        )
    }
}

private class AnimalReadProbeRepository : AnimalRepository {
    var animal: Animal? = null

    override suspend fun getAnimalById(id: Long): Animal? = animal

    override fun getAllAnimals(userId: Long): Flow<List<Animal>> = error("Unexpected")

    override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = error("Unexpected")

    override suspend fun insertAnimal(animal: Animal): Long = error("Unexpected")

    override suspend fun updateAnimal(animal: Animal): Boolean = error("Unexpected")

    override suspend fun deleteAnimal(id: Long) = error("Unexpected")
}

private class NotificationReadProbeRepository : NotificacionRepository {
    var byId: Map<Long, Notificacion> = emptyMap()

    override suspend fun getNotificationById(id: Long): Notificacion? = byId[id]

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> = error("Unexpected")

    override suspend fun insertNotification(notificacion: Notificacion): Long = error("Unexpected")

    override suspend fun updateNotification(notificacion: Notificacion) = error("Unexpected")

    override suspend fun deleteNotification(id: Long) = error("Unexpected")
}

private class LinkReadProbeRepository : NotificacionxAnimalRepository {
    var links: List<NotificacionxAnimalEntity> = emptyList()
    var calls: Int = 0

    override suspend fun getNotificationsOfAnimal(idAnimal: Long): List<NotificacionxAnimalEntity> {
        calls += 1
        return links
    }

    override suspend fun assignNotificationToAnimal(idAnimal: Long, idNotificacion: Long) = Unit

    override suspend fun removeAssignment(idAnimal: Long, idNotificacion: Long) = Unit

    override suspend fun getAnimalsWithNotification(
        idNotificacion: Long,
    ): List<NotificacionxAnimalEntity> = error("Unexpected")
}
