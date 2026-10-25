package com.proyecto.ganapp.ui.features.notifications

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun selectedNotificationEpochDay(year: Int, month: Int, dayOfMonth: Int): Long {
    return LocalDate.of(year, month, dayOfMonth).toEpochDay()
}

fun formatNotificationEpochDay(epochDay: Long): String {
    return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
