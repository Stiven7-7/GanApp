package com.proyecto.ganapp.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import com.proyecto.ganapp.domain.service.ScheduleAlarmResult
import java.util.concurrent.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidNotificationScheduleAlarmServiceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val service = AndroidNotificationScheduleAlarmService(context)
    private val firstId = 101L
    private val secondId = 102L
    private val testTokens = mutableListOf<PendingIntent>()

    @After
    fun cleanupOnlyNewTestAlarms() {
        service.cancel(firstId)
        service.cancel(secondId)
        service.cancel(Long.MAX_VALUE)
        testTokens.forEach { it.cancel() }
    }

    @Test
    fun idsInvalidosNoAccedenAndroidNiCreanPendingIntent() {
        val probe = AccessProbe(context)
        val candidate = AndroidNotificationScheduleAlarmService(probe)
        listOf(0L, -1L).forEach { id ->
            assertEquals(ScheduleAlarmResult.InvalidScheduleId, candidate.schedule(id, future()))
            assertEquals(ScheduleAlarmCancelResult.InvalidScheduleId, candidate.cancel(id))
            assertNull(find(id))
        }
        assertEquals(0, probe.calls)
    }

    @Test
    fun triggerNoPositivoPasadoOActualNoAccedeAndroid() {
        val probe = AccessProbe(context)
        val candidate = AndroidNotificationScheduleAlarmService(probe)
        listOf(Long.MIN_VALUE, -1L, 0L, System.currentTimeMillis() - 1L, System.currentTimeMillis()).forEach { trigger ->
            assertEquals(ScheduleAlarmResult.InvalidTriggerTime, candidate.schedule(firstId, trigger))
        }
        assertEquals(0, probe.calls)
        assertNull(find(firstId))
    }

    @Test
    fun scheduleValidoRegistraAlarmaAndroidInexactaYWakeup() {
        val probe = AccessProbe(context)
        val candidate = AndroidNotificationScheduleAlarmService(probe)
        assertEquals(ScheduleAlarmResult.Scheduled, candidate.schedule(firstId, future()))
        assertEquals(1, probe.calls)
        assertNotNull(find(firstId))
        val block = alarmBlock(firstId)
        assertTrue(block, block.contains("RTC_WAKEUP"))
        assertTrue(block, block.contains("window="))
        assertFalse(block, block.contains("window=0 "))
    }

    @Test
    fun mismoIdProduceMismoPendingIntentRealEInmutable() {
        assertEquals(ScheduleAlarmResult.Scheduled, service.schedule(firstId, future()))
        val first = find(firstId)!!
        assertEquals(ScheduleAlarmResult.Scheduled, service.schedule(firstId, future(2)))
        val second = find(firstId)!!
        assertEquals(first, second)
        assertTrue(first.isImmutable)
        val a = NotificationScheduleAlarmIntentContract.intent(context, firstId)
        val b = NotificationScheduleAlarmIntentContract.intent(context, firstId)
        assertTrue(a.filterEquals(b))
        assertEquals(NotificationScheduleAlarmReceiver::class.java.name, a.component!!.className)
    }

    @Test
    fun idsDistintosTienenIdentidadesAndroidDistintas() {
        service.schedule(firstId, future())
        service.schedule(secondId, future())
        assertNotEquals(find(firstId), find(secondId))
        assertFalse(NotificationScheduleAlarmIntentContract.intent(context, firstId).filterEquals(
            NotificationScheduleAlarmIntentContract.intent(context, secondId),
        ))
    }

    @Test
    fun rescheduleReemplazaUnaAlarmaSinTocarOtroId() {
        service.schedule(firstId, future())
        service.schedule(secondId, future(2))
        val firstBefore = find(firstId)
        val secondBefore = find(secondId)
        val secondAlarmBefore = alarmConfiguration(secondId)
        val replacement = future(3)
        service.schedule(firstId, replacement)
        assertTrue(alarmBlock(firstId).contains("origWhen $replacement "))
        assertEquals(firstBefore, find(firstId))
        assertEquals(secondBefore, find(secondId))
        assertEquals(secondAlarmBefore, alarmConfiguration(secondId))
        assertEquals(1, activeAlarmCount(firstId))
        assertEquals(1, activeAlarmCount(secondId))
    }

    @Test
    fun cancelExistenteEliminaSoloLaIdentidadIndicada() {
        service.schedule(firstId, future())
        service.schedule(secondId, future(2))
        assertEquals(ScheduleAlarmCancelResult.Cancelled, service.cancel(firstId))
        assertNull(find(firstId))
        assertNotNull(find(secondId))
        assertEquals(0, activeAlarmCount(firstId))
        assertEquals(1, activeAlarmCount(secondId))
    }

    @Test
    fun cancelAusenteEsIdempotenteYNoCreaToken() {
        assertNull(find(firstId))
        assertEquals(ScheduleAlarmCancelResult.Cancelled, service.cancel(firstId))
        assertEquals(ScheduleAlarmCancelResult.Cancelled, service.cancel(firstId))
        assertNull(find(firstId))
    }

    @Test
    fun excepcionAndroidDeScheduleConservaCausa() {
        val error = SecurityException("schedule access")
        val candidate = AndroidNotificationScheduleAlarmService(AccessProbe(context, error))
        assertSame(error, (candidate.schedule(firstId, future()) as ScheduleAlarmResult.Failure).cause)
        assertNull(find(firstId))
    }

    @Test
    fun excepcionAndroidDeCancelConservaCausa() {
        val error = IllegalStateException("cancel access")
        val candidate = AndroidNotificationScheduleAlarmService(AccessProbe(context, error))
        assertSame(error, (candidate.cancel(firstId) as ScheduleAlarmCancelResult.Failure).cause)
    }

    @Test
    fun cancelacionExcepcionalNoSeConvierteEnFailure() {
        val cancellation = CancellationException("cancel")
        val candidate = AndroidNotificationScheduleAlarmService(AccessProbe(context, cancellation))
        try {
            candidate.schedule(firstId, future())
            throw AssertionError("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        try {
            candidate.cancel(firstId)
            throw AssertionError("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun payloadExactoSoloContieneScheduleIdSinSnapshots() {
        val intent = NotificationScheduleAlarmIntentContract.intent(context, firstId)
        assertEquals(setOf("scheduleId"), intent.extras!!.keySet())
        assertEquals(firstId, intent.getLongExtra("scheduleId", 0L))
        assertFalse(intent.hasExtra(NotificationAlarmIntentContract.EXTRA_NOTIFICATION_ID))
        listOf("title", "message", "type", "titulo", "tipo", "templateId", "animalId").forEach {
            assertFalse(intent.hasExtra(it))
        }
        assertEquals("com.proyecto.ganapp.action.NOTIFICATION_SCHEDULE_ALARM", intent.action)
        assertEquals(Uri.parse("ganapp://notification-schedule-alarm/101"), intent.data)
    }

    @Test
    fun namespaceLegacyYNuevoNoColisionanEnPendingIntentReal() {
        val legacy = NotificationAlarmIdentity.of(firstId)
        val legacyIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = legacy.action
            data = Uri.parse(legacy.dataUri)
        }
        val newIntent = NotificationScheduleAlarmIntentContract.intent(context, firstId)
        assertFalse(legacyIntent.filterEquals(newIntent))
        assertNotEquals(legacyIntent.action, newIntent.action)
        assertNotEquals(legacyIntent.data, newIntent.data)
        assertNotEquals(legacyIntent.component, newIntent.component)
        // Conserva cualquier token legacy preexistente sin actualizarlo ni cancelarlo.
        val existingLegacy = PendingIntent.getBroadcast(
            context, 0, legacyIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        val legacyToken = existingLegacy ?: PendingIntent.getBroadcast(
            context, 0, legacyIntent, PendingIntent.FLAG_IMMUTABLE,
        ).also { testTokens += it } // Token de prueba nuevo, sin alarma asociada.
        val newToken = PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_IMMUTABLE)
        testTokens += newToken
        assertNotEquals(legacyToken, newToken)
    }

    @Test
    fun longMaxMantieneIdentidadCompletaSinNarrowing() {
        assertEquals(ScheduleAlarmResult.Scheduled, service.schedule(Long.MAX_VALUE, future()))
        assertNotNull(find(Long.MAX_VALUE))
        assertEquals("ganapp://notification-schedule-alarm/${Long.MAX_VALUE}",
            NotificationScheduleAlarmIntentContract.intent(context, Long.MAX_VALUE).dataString)
    }

    @Test
    fun receiverRegistradoEsInternoEInerte() {
        val component = NotificationScheduleAlarmIntentContract.intent(context, firstId).component!!
        @Suppress("DEPRECATION")
        val info = context.packageManager.getReceiverInfo(component, 0)
        assertFalse(info.exported)
        val probe = AccessProbe(context, AssertionError("Receiver must remain inert"))
        NotificationScheduleAlarmReceiver().onReceive(
            probe, NotificationScheduleAlarmIntentContract.intent(context, firstId),
        )
        assertEquals(0, probe.calls)
    }

    private fun future(hours: Int = 1) = System.currentTimeMillis() + hours * 3_600_000L

    private fun find(id: Long): PendingIntent? = PendingIntent.getBroadcast(
        context, NotificationScheduleAlarmIdentity.REQUEST_CODE,
        NotificationScheduleAlarmIntentContract.intent(context, id),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).bufferedReader().use { it.readText() }

    private fun recordKey(id: Long): String? {
        val uri = Regex(Regex.escape(NotificationScheduleAlarmIntentContract.dataUri(id)) + "(?:\\s|$)")
        val record = Regex(Regex.escape("PendingIntentRecord{") + "([a-f0-9]+) " +
            Regex.escape(context.packageName + " broadcastIntent}"))
        var key: String? = null
        for (line in shell("dumpsys activity intents").lines()) {
            if (line.contains("PendingIntentRecord{")) key = record.find(line)?.groupValues?.get(1)
            if (key != null && uri.containsMatchIn(line)) return key
        }
        return null
    }

    // Relaciona URI -> registro PendingIntent -> alarma activa, excluyendo históricos.
    private fun activeAlarmBlocks(id: Long): List<String> {
        val key = recordKey(id) ?: return emptyList()
        val dump = shell("dumpsys alarm")
        val start = Regex("(?m)^\\s*\\d+ pending alarms:").find(dump)?.range?.first
            ?: error("Unsupported dumpsys alarm format: missing active section")
        val end = dump.indexOf("LazyAlarmStore stats:", start)
        check(end > start) { "Unsupported dumpsys alarm format: missing active section end" }
        return dump.substring(start, end)
            .split(Regex("(?m)(?=^\\s{4}(?:RTC_WAKEUP|RTC|ELAPSED_WAKEUP|ELAPSED) #)"))
            .filter { it.contains("PendingIntentRecord{$key ") }
    }

    private fun activeAlarmCount(id: Long): Int = activeAlarmBlocks(id).size

    private fun alarmBlock(id: Long): String = activeAlarmBlocks(id).single()

    private fun alarmConfiguration(id: Long): String =
        alarmBlock(id).lineSequence().first { it.trimStart().startsWith("type=") }.trim()

    private class AccessProbe(base: Context, private val failure: Throwable? = null) : ContextWrapper(base) {
        var calls = 0
        override fun getSystemService(name: String): Any? {
            calls++
            failure?.let { throw it }
            return super.getSystemService(name)
        }
    }
}
