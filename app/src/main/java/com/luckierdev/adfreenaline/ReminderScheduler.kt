package com.luckierdev.adfreenaline

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class ReminderScheduler(private val context: Context) {
    fun schedule(enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val slots = listOf(9 to 0, 14 to 0, 20 to 0)
        slots.forEachIndexed { index, (hour, minute) ->
            val intent = Intent(context, RunReminderReceiver::class.java).apply {
                putExtra("slot", index)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            if (!enabled) return@forEachIndexed
            val triggerAtMs = nextTrigger(hour, minute)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }
}
