package com.proyecto.ganapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Target técnico inerte. No muestra notificaciones, consulta datos ni reprograma alarmas. */
class NotificationScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
