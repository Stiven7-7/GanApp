package com.proyecto.ganapp.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.proyecto.ganapp.domain.service.LegacyNotificationAlarmCanceller
import com.proyecto.ganapp.domain.service.ScheduleAlarmCancelResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Usa la identidad PendingIntent anterior a v11. No llama a [android.app.AlarmManager.set]. */
class AndroidLegacyNotificationAlarmCanceller @Inject constructor(
    @ApplicationContext private val context: Context,
) : LegacyNotificationAlarmCanceller {
    override fun cancel(notificationId: Long): ScheduleAlarmCancelResult {
        if (notificationId <= 0L) return ScheduleAlarmCancelResult.InvalidScheduleId
        return try {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val identity = NotificationAlarmIdentity.of(notificationId)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                identity.requestCode,
                Intent(context, NotificationReceiver::class.java).apply {
                    action = identity.action
                    data = Uri.parse(identity.dataUri)
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null) {
                manager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            ScheduleAlarmCancelResult.Cancelled
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            ScheduleAlarmCancelResult.Failure(exception)
        }
    }
}
