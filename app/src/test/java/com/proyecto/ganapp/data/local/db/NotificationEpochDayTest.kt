package com.proyecto.ganapp.data.local.db

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationEpochDayTest {

    @Test
    fun medianocheEnBogota_conservaElDiaCalendario() {
        val bogota = ZoneId.of("America/Bogota")
        val date = LocalDate.of(2026, 1, 31)
        val epochMillis = date.atStartOfDay(bogota).toInstant().toEpochMilli()

        assertEquals(date.toEpochDay(), epochMillisToEpochDay(epochMillis, bogota))
    }

    @Test
    fun medianocheEnLosAngeles_conservaElDiaCalendario() {
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val date = LocalDate.of(2026, 6, 15)
        val epochMillis = date.atStartOfDay(losAngeles).toInstant().toEpochMilli()

        assertEquals(date.toEpochDay(), epochMillisToEpochDay(epochMillis, losAngeles))
    }
}
