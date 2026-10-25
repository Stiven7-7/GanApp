package com.proyecto.ganapp.domain.service

import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object NotificationRecurrenceEngine {

    private val strictTime = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")

    fun nextOccurrence(
        recurrence: RecurrenciaNotificacion,
        fechaInicio: Long,
        fechaFin: Long,
        hora: String,
        afterMillis: Long,
        zoneId: ZoneId,
    ): Long? {
        val time = parseStrictTime(hora) ?: return null
        return try {
            val startDate = LocalDate.ofEpochDay(fechaInicio)
            val endDate = LocalDate.ofEpochDay(fechaFin)
            if (endDate.isBefore(startDate)) return null
            when (recurrence) {
                RecurrenciaNotificacion.NO ->
                    nextSingle(startDate, time, afterMillis, zoneId)
                RecurrenciaNotificacion.DIARIA ->
                    nextByDayStep(startDate, endDate, 1L, time, afterMillis, zoneId)
                RecurrenciaNotificacion.SEMANAL ->
                    nextByDayStep(startDate, endDate, 7L, time, afterMillis, zoneId)
                RecurrenciaNotificacion.CADA_15_DIAS ->
                    nextByDayStep(startDate, endDate, 15L, time, afterMillis, zoneId)
                RecurrenciaNotificacion.MENSUAL ->
                    nextMonthly(startDate, endDate, time, afterMillis, zoneId)
                RecurrenciaNotificacion.ANUAL ->
                    nextAnnual(startDate, endDate, time, afterMillis, zoneId)
            }
        } catch (exception: DateTimeException) {
            null
        }
    }

    private fun nextSingle(
        startDate: LocalDate,
        time: LocalTime,
        afterMillis: Long,
        zoneId: ZoneId,
    ): Long? {
        val occurrence = toEpoch(startDate, time, zoneId)
        if (occurrence <= afterMillis) return null
        return occurrence
    }

    private fun nextByDayStep(
        startDate: LocalDate,
        endDate: LocalDate,
        stepDays: Long,
        time: LocalTime,
        afterMillis: Long,
        zoneId: ZoneId,
    ): Long? {
        val afterDate = Instant.ofEpochMilli(afterMillis).atZone(zoneId).toLocalDate()
        val elapsed = ChronoUnit.DAYS.between(startDate, afterDate)
        val steps = if (elapsed <= 0L) 0L else elapsed / stepDays
        var candidate = startDate.plusDays(steps * stepDays)
        if (toEpoch(candidate, time, zoneId) <= afterMillis) {
            candidate = candidate.plusDays(stepDays)
        }
        return withinEnd(candidate, endDate, time, zoneId)
    }

    private fun nextMonthly(
        startDate: LocalDate,
        endDate: LocalDate,
        time: LocalTime,
        afterMillis: Long,
        zoneId: ZoneId,
    ): Long? {
        val anchorDay = startDate.dayOfMonth
        val startMonth = YearMonth.from(startDate)
        val afterDate = Instant.ofEpochMilli(afterMillis).atZone(zoneId).toLocalDate()
        val months = if (afterDate.isBefore(startDate)) {
            0L
        } else {
            ChronoUnit.MONTHS.between(startMonth, YearMonth.from(afterDate)).coerceAtLeast(0L)
        }
        var candidate = monthlyDate(startMonth.plusMonths(months), anchorDay)
        if (toEpoch(candidate, time, zoneId) <= afterMillis) {
            candidate = monthlyDate(startMonth.plusMonths(months + 1L), anchorDay)
        }
        return withinEnd(candidate, endDate, time, zoneId)
    }

    private fun nextAnnual(
        startDate: LocalDate,
        endDate: LocalDate,
        time: LocalTime,
        afterMillis: Long,
        zoneId: ZoneId,
    ): Long? {
        val anchorMonth = startDate.month
        val anchorDay = startDate.dayOfMonth
        val afterDate = Instant.ofEpochMilli(afterMillis).atZone(zoneId).toLocalDate()
        val year = if (afterDate.isBefore(startDate)) {
            startDate.year
        } else {
            maxOf(startDate.year, afterDate.year)
        }
        var candidate = annualDate(year, anchorMonth, anchorDay)
        if (toEpoch(candidate, time, zoneId) <= afterMillis) {
            candidate = annualDate(year + 1, anchorMonth, anchorDay)
        }
        return withinEnd(candidate, endDate, time, zoneId)
    }

    private fun monthlyDate(month: YearMonth, anchorDay: Int): LocalDate {
        return month.atDay(minOf(anchorDay, month.lengthOfMonth()))
    }

    private fun annualDate(year: Int, month: Month, anchorDay: Int): LocalDate {
        val yearMonth = YearMonth.of(year, month)
        return yearMonth.atDay(minOf(anchorDay, yearMonth.lengthOfMonth()))
    }

    private fun withinEnd(
        candidate: LocalDate,
        endDate: LocalDate,
        time: LocalTime,
        zoneId: ZoneId,
    ): Long? {
        if (candidate.isAfter(endDate)) return null
        return toEpoch(candidate, time, zoneId)
    }

    private fun toEpoch(date: LocalDate, time: LocalTime, zoneId: ZoneId): Long {
        return date.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun parseStrictTime(hora: String): LocalTime? {
        val match = strictTime.matchEntire(hora) ?: return null
        return LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
}
