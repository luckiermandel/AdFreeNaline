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
            "Run Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val messages = listOf(
            "Your running shoes filed a missing person report.",
            "Streak check: your future self wants a tiny run today.",
            "One short run today beats 100 excuses tomorrow.",
            "Your couch is winning. Are we okay with that?",
            "Streak gremlin detected. Run now to scare it away.",
            "Plot twist: 15 minutes today keeps your streak legendary.",
            "Reminder #${(intent?.getIntExtra("slot", 0) ?: 0) + 1}/3: go protect that streak.",
            "Breaking news: your legs are still accepting appointments.",
            "Your playlist is warmed up. Are you?",
            "Tiny run, huge hero arc.",
            "Run now, brag later.",
            "Even a slow jog beats a fast regret.",
            "The route misses you. It keeps asking about you.",
            "Your sneakers are bored. End their suffering.",
            "Future-you just sent a thank-you in advance.",
            "Go collect those endorphins before they expire.",
            "A short run today keeps the slump away."
        )
        val msg = messages.random()
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("${context.getString(R.string.app_name)}: move those legs")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
