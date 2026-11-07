package com.proyecto.ganapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Destino residual de alarmas legacy. No consulta tablas eliminadas ni reprograma. */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
