package com.proyecto.ganapp.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.proyecto.ganapp.domain.model.Notificacion
import java.util.Calendar
import java.util.Locale

object NotificationScheduler {

    fun schedule(context: Context, noti: Notificacion) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Día + hora base
        val baseCal: Calendar = Calendar.getInstance().apply {
            timeInMillis = noti.fechaInicio   // día de inicio

            val (hour, minute) = parseHora(noti.hora)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val hasMultipleDoses =
            (noti.dosisPorDia ?: 0) > 1 && (noti.intervaloHoras ?: 0) > 0

        if (hasMultipleDoses) {
            scheduleMultipleDoses(context, alarmManager, noti, baseCal)
        } else {
            scheduleSingleDose(context, alarmManager, noti, baseCal.timeInMillis)
        }
    }

    /* ───────────────────────── 1 SOLA DOSIS ───────────────────────── */

    private fun scheduleSingleDose(
        context: Context,
        alarmManager: AlarmManager,
        noti: Notificacion,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", "Recordatorio GanApp")
            putExtra(
                "message",
                "Recordatorio - la ${noti.tipo.lowercase()} '${noti.nombre}' se debe suministrar."
            )
        }

        val requestCode = (noti.idNotificacion.takeIf { it != 0L }
            ?: System.currentTimeMillis()).toInt()

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        when (noti.seRepite.uppercase(Locale.ROOT)) {
            "DIARIA" -> alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            "SEMANAL" -> alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )

            "CADA 15 DÍAS", "CADA 15 DIAS" -> alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                AlarmManager.INTERVAL_DAY * 15,
                pendingIntent
            )

            "MENSUAL" -> alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                AlarmManager.INTERVAL_DAY * 30,
                pendingIntent
            )

            else -> { // NO se repite
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // 🔁 No requiere SCHEDULE_EXACT_ALARM
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    /* ─────────────────────── VARIAS DOSIS POR DÍA ─────────────────────── */

    private fun scheduleMultipleDoses(
        context: Context,
        alarmManager: AlarmManager,
        noti: Notificacion,
        baseCal: Calendar
    ) {
        val dosisPorDia = noti.dosisPorDia ?: 1
        val intervaloHoras = noti.intervaloHoras ?: 0

        for (i in 0 until dosisPorDia) {
            val cal = baseCal.clone() as Calendar
            cal.add(Calendar.HOUR_OF_DAY, i * intervaloHoras)
            val triggerAtMillis = cal.timeInMillis

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", "GanApp - Dosis ${i + 1}")
                putExtra(
                    "message",
                    "Recordatorio - dosis ${i + 1} de la ${noti.tipo.lowercase()} '${noti.nombre}'."
                )
            }

            val baseId = (noti.idNotificacion.takeIf { it != 0L }
                ?: System.currentTimeMillis()).toInt()
            val requestCode = baseId + i

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flags
            )

            if (noti.seRepite.uppercase(Locale.ROOT) == "DIARIA") {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // 🔁 No requiere SCHEDULE_EXACT_ALARM
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    private fun parseHora(hora: String): Pair<Int, Int> {
        val parts = hora.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h to m
    }
}
