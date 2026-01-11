package com.proyecto.ganapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "GanApp"
        val message = intent.getStringExtra("message")
            ?: "Tienes un recordatorio pendiente en tu ganado."

        NotificationHelper.show(context, title, message)
    }
}
