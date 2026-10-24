package com.proyecto.ganapp.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class AndroidNotificationAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationAlarmScheduler {

    override fun scheduleNext(request: NotificationAlarmRequest): NotificationScheduleResult {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                NotificationAlarmIdentity.of(request.notificationId).requestCode,
                scheduleIntent(request),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                request.triggerAtMillis,
                pendingIntent,
            )
            NotificationScheduleResult.Scheduled(
                notificationId = request.notificationId,
                triggerAtMillis = request.triggerAtMillis,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            NotificationScheduleResult.Failure(
                notificationId = request.notificationId,
                cause = exception,
            )
        }
    }

    override fun cancel(notificationId: Long): NotificationCancelResult {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent: PendingIntent? = PendingIntent.getBroadcast(
                context,
                NotificationAlarmIdentity.of(notificationId).requestCode,
                identityIntent(notificationId),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            NotificationCancelResult.Cancelled(notificationId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            NotificationCancelResult.Failure(
                notificationId = notificationId,
                cause = exception,
            )
        }
    }

    private fun scheduleIntent(request: NotificationAlarmRequest): Intent {
        return identityIntent(request.notificationId).apply {
            putExtra(NotificationAlarmIntentContract.EXTRA_NOTIFICATION_ID, request.notificationId)
            putExtra(NotificationAlarmIntentContract.EXTRA_TITLE, request.title)
            putExtra(NotificationAlarmIntentContract.EXTRA_MESSAGE, request.message)
        }
    }

    private fun identityIntent(notificationId: Long): Intent {
        val identity = NotificationAlarmIdentity.of(notificationId)
        return Intent(context, NotificationReceiver::class.java).apply {
            action = identity.action
            data = Uri.parse(identity.dataUri)
        }
    }
}
