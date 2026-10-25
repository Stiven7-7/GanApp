package com.proyecto.ganapp.ui.features.notifications

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationCalendarDateTest {

    @Test
    fun seleccionaElEpochDayDe31Enero2026() {
        val expected = LocalDate.of(2026, 1, 31).toEpochDay()

        assertEquals(expected, selectedNotificationEpochDay(2026, 1, 31))
    }

    @Test
    fun formateaEpochDayComoDiaMesAnio() {
        val epochDay = LocalDate.of(2026, 1, 31).toEpochDay()

        assertEquals("31/01/2026", formatNotificationEpochDay(epochDay))
    }
}
