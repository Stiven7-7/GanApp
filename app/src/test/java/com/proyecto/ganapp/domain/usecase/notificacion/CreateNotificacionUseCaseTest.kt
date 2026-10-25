package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.model.TipoNotificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CreateNotificacionUseCaseTest {

    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.of(2026, 6, 15)
    private val start = day.toEpochDay()
    private val now = day.minusDays(1).atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()

    private lateinit var repository: FakeNotificacionRepository
    private lateinit var useCase: CreateNotificacionUseCase

    @Before
    fun setUp() {
        repository = FakeNotificacionRepository()
        useCase = CreateNotificacionUseCase(repository)
    }

    @Test
    fun idPositivo_retornaPersistedYEntregaLaNotificacionNormalizada() = runTest {
        repository.nextId = 42L
        val request = validRequest(nombre = " Vacuna ")

        val result = useCase(request, now)

        assertEquals(CreateNotificationPersistenceResult.Persisted(42L), result)
        assertEquals(1, repository.insertCalls)
        val inserted = repository.inserted
        assertEquals("Vacuna", inserted?.nombre)
        assertEquals(" Vacuna ", request.nombre)
        assertEquals("Vacunación", inserted?.tipo)
        assertEquals("NO", inserted?.seRepite)
        assertEquals(0L, inserted?.idNotificacion)
        assertNull(inserted?.dosisPorDia)
        assertNull(inserted?.intervaloHoras)
        assertEquals(start, inserted?.fechaInicio)
        assertEquals(start, inserted?.fechaFin)
        assertEquals("08:30", inserted?.hora)
        assertEquals(7L, inserted?.idUsuario)
    }

    @Test
    fun requestInvalido_retornaValidationFailureSinPersistir() = runTest {
        val result = useCase(validRequest(nombre = ""), now)

        assertEquals(
            CreateNotificationPersistenceResult.ValidationFailure(
                setOf(CreateNotificationValidationError.BLANK_NAME),
            ),
            result,
        )
        assertEquals(0, repository.insertCalls)
        assertNull(repository.inserted)
    }

    @Test
    fun idCero_retornaPersistenceFailure() = runTest {
        repository.nextId = 0L

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationPersistenceResult.PersistenceFailure(), result)
        assertEquals(1, repository.insertCalls)
    }

    @Test
    fun idNegativo_retornaPersistenceFailure() = runTest {
        repository.nextId = -1L

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationPersistenceResult.PersistenceFailure(), result)
        assertEquals(1, repository.insertCalls)
    }

    @Test
    fun excepcionDelRepository_retornaPersistenceFailureConLaCausaOriginal() = runTest {
        val error = IllegalStateException("db error")
        repository.insertError = error

        val result = useCase(validRequest(), now)

        assertEquals(CreateNotificationPersistenceResult.PersistenceFailure(error), result)
        assertSame(error, (result as CreateNotificationPersistenceResult.PersistenceFailure).cause)
    }

    @Test
    fun cancellationException_sePropaga() = runTest {
        val cancellation = CancellationException("cancelled")
        repository.insertError = cancellation

        try {
            useCase(validRequest(), now)
            fail("La cancelación debió propagarse")
        } catch (exception: CancellationException) {
            assertSame(cancellation, exception)
        }
    }

    private fun validRequest(
        nombre: String = "Vacuna",
    ): CreateNotificationRequest {
        return CreateNotificationRequest(
            nombre = nombre,
            tipo = TipoNotificacion.VACUNACION,
            fechaInicio = start,
            fechaFin = start,
            hora = "08:30",
            recurrencia = RecurrenciaNotificacion.NO,
            idUsuario = 7L,
        )
    }
}

private class FakeNotificacionRepository : NotificacionRepository {
    var nextId: Long = 0L
    var insertError: Throwable? = null
    var insertCalls: Int = 0
    var inserted: Notificacion? = null

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> = emptyFlow()

    override suspend fun insertNotification(notificacion: Notificacion): Long {
        insertCalls += 1
        inserted = notificacion
        insertError?.let { throw it }
        return nextId
    }

    override suspend fun updateNotification(notificacion: Notificacion) = Unit

    override suspend fun deleteNotification(id: Long) = Unit

    override suspend fun getNotificationById(id: Long): Notificacion? = null
}
