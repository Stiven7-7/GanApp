package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AssignNotificationToAnimalUseCaseTest {

    private lateinit var animals: AssignProbeAnimalRepository
    private lateinit var notifications: AssignProbeNotificacionRepository
    private lateinit var links: AssignProbeLinkRepository
    private lateinit var useCase: AssignNotificationToAnimalUseCase

    @Before
    fun setUp() {
        animals = AssignProbeAnimalRepository()
        notifications = AssignProbeNotificacionRepository()
        links = AssignProbeLinkRepository()
        useCase = AssignNotificationToAnimalUseCase(
            animalRepository = animals,
            notificationRepository = notifications,
            linkRepository = links,
        )
    }

    @Test
    fun inputInvalido_noConsultaRepositorios() = runTest {
        listOf(
            Triple(0L, 1L, 1L),
            Triple(1L, 0L, 1L),
            Triple(1L, 1L, -1L),
        ).forEach { (userId, animalId, notificationId) ->
            val result = useCase(userId, animalId, notificationId)
            assertEquals(AssignNotificationToAnimalResult.InvalidInput, result)
        }
        assertEquals(0, animals.calls)
        assertEquals(0, notifications.calls)
        assertEquals(0, links.assignCalls)
    }

    @Test
    fun animalInexistente_noCreaRelacion() = runTest {
        animals.animal = null

        val result = useCase(userId = 7L, animalId = 4L, notificationId = 9L)

        assertEquals(AssignNotificationToAnimalResult.AnimalNotFound, result)
        assertEquals(0, notifications.calls)
        assertEquals(0, links.assignCalls)
    }

    @Test
    fun notificacionInexistente_noCreaRelacion() = runTest {
        animals.animal = animal(ownerId = 7L)
        notifications.notification = null

        val result = useCase(userId = 7L, animalId = 4L, notificationId = 9L)

        assertEquals(AssignNotificationToAnimalResult.NotificationNotFound, result)
        assertEquals(0, links.assignCalls)
    }

    @Test
    fun animalDeOtroUsuario_rechazaSinRelacion() = runTest {
        animals.animal = animal(ownerId = 8L)
        notifications.notification = notification(ownerId = 7L)

        val result = useCase(userId = 7L, animalId = 4L, notificationId = 9L)

        assertEquals(AssignNotificationToAnimalResult.OwnershipMismatch, result)
        assertEquals(0, links.assignCalls)
    }

    @Test
    fun notificacionDeOtroUsuario_rechazaSinRelacion() = runTest {
        animals.animal = animal(ownerId = 7L)
        notifications.notification = notification(ownerId = 8L)

        val result = useCase(userId = 7L, animalId = 4L, notificationId = 9L)

        assertEquals(AssignNotificationToAnimalResult.OwnershipMismatch, result)
        assertEquals(0, links.assignCalls)
    }

    @Test
    fun ambosCoincidenEntreSiPeroNoConElUsuario_rechaza() = runTest {
        animals.animal = animal(ownerId = 8L)
        notifications.notification = notification(ownerId = 8L)

        val result = useCase(userId = 7L, animalId = 4L, notificationId = 9L)

        assertEquals(AssignNotificationToAnimalResult.OwnershipMismatch, result)
        assertEquals(0, links.assignCalls)
    }

    @Test
    fun ambosPertenecenAlUsuario_asignaUnaVez() = runTest {
        animals.animal = animal(ownerId = 7L)
        notifications.notification = notification(ownerId = 7L)

        val result = useCase(userId = 7L, animalId = 4L, notificationId = 9L)

        assertEquals(AssignNotificationToAnimalResult.Success, result)
        assertEquals(1, links.assignCalls)
        assertEquals(4L, links.lastAnimalId)
        assertEquals(9L, links.lastNotificationId)
    }

    @Test
    fun falloDelRepositorioDeAsignacion_devuelveFailure() = runTest {
        val cause = IllegalStateException("link")
        animals.animal = animal(ownerId = 7L)
        notifications.notification = notification(ownerId = 7L)
        links.assignError = cause

        val result = useCase(userId = 7L, animalId = 4L, notificationId = 9L)

        assertEquals(AssignNotificationToAnimalResult.Failure(cause), result)
        assertEquals(1, links.assignCalls)
    }

    @Test
    fun cancellationException_sePropaga() = runTest {
        val cancellation = CancellationException("assign")
        animals.animal = animal(ownerId = 7L)
        notifications.notification = notification(ownerId = 7L)
        links.assignError = cancellation

        try {
            useCase(userId = 7L, animalId = 4L, notificationId = 9L)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
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

    private fun notification(ownerId: Long): Notificacion {
        return Notificacion(
            idNotificacion = 9L,
            nombre = "Vacuna",
            tipo = "Vacunación",
            fechaInicio = 20_000L,
            fechaFin = 20_000L,
            seRepite = "NO",
            hora = "08:00",
            idUsuario = ownerId,
        )
    }
}

private class AssignProbeAnimalRepository : AnimalRepository {
    var animal: Animal? = null
    var calls: Int = 0

    override suspend fun getAnimalById(id: Long): Animal? {
        calls += 1
        return animal
    }

    override fun getAllAnimals(userId: Long): Flow<List<Animal>> = error("Unexpected")

    override fun getAnimalsByUser(userId: Long): Flow<List<Animal>> = error("Unexpected")

    override fun getFavoriteAnimalsByUser(userId: Long): Flow<List<Animal>> = error("Unexpected")

    override suspend fun insertAnimal(animal: Animal): Long = error("Unexpected")

    override suspend fun updateAnimal(animal: Animal): Boolean = error("Unexpected")

    override suspend fun deleteAnimal(id: Long) = error("Unexpected")
}

private class AssignProbeNotificacionRepository : NotificacionRepository {
    var notification: Notificacion? = null
    var calls: Int = 0

    override suspend fun getNotificationById(id: Long): Notificacion? {
        calls += 1
        return notification
    }

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> = error("Unexpected")

    override suspend fun insertNotification(notificacion: Notificacion): Long = error("Unexpected")

    override suspend fun updateNotification(notificacion: Notificacion) = error("Unexpected")

    override suspend fun deleteNotification(id: Long) = error("Unexpected")
}

private class AssignProbeLinkRepository : NotificacionxAnimalRepository {
    var assignCalls: Int = 0
    var lastAnimalId: Long? = null
    var lastNotificationId: Long? = null
    var assignError: Throwable? = null

    override suspend fun assignNotificationToAnimal(idAnimal: Long, idNotificacion: Long) {
        assignCalls += 1
        lastAnimalId = idAnimal
        lastNotificationId = idNotificacion
        assignError?.let { throw it }
    }

    override suspend fun removeAssignment(idAnimal: Long, idNotificacion: Long) = Unit

    override suspend fun getNotificationIdsOfAnimal(idAnimal: Long): List<Long> = error("Unexpected")

    override suspend fun getAnimalIdsWithNotification(idNotificacion: Long): List<Long> {
        error("Unexpected")
    }
}
