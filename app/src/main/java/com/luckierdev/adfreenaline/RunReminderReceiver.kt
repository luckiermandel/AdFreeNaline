package com.luckierdev.adfreenaline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar

class RunReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val history = RunHistoryRepository(context).history.value
        val now = System.currentTimeMillis()
        val startedToday = history.any { sameDay(it.timestampMs, now) }
        if (startedToday) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "run_reminders"
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.channel_run_reminders),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val msg = context.resources.getStringArray(R.array.reminder_messages).random()
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.notif_reminder_title, context.getString(R.string.app_name)))
            .setContentText(msg)
            .setSmallIcon(R.drawable.ic_stat_run)
            .setAutoCancel(true)
            .build()
        manager.notify(4201, notification)
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
