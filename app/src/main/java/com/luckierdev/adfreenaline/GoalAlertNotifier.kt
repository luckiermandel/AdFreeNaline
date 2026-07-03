package com.luckierdev.adfreenaline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat

class GoalAlertNotifier(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun notifyGoalReached(title: String, message: String, muted: Boolean, soundUri: String?) {
        ensureChannel()
        if (!muted) {
            playSound(soundUri)
        }
        val uri = resolveSoundUri(soundUri)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_goal)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (muted) {
            builder.setSilent(true)
        } else {
            builder.setSound(uri)
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun previewSound(muted: Boolean, soundUri: String?) {
        if (muted) return
        playSound(soundUri)
    }

    fun soundLabel(muted: Boolean, soundUri: String?): String {
        if (muted) return context.getString(R.string.sound_muted)
        val default = context.getString(R.string.sound_default)
        val uri = resolveSoundUri(soundUri) ?: return default
        return runCatching {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: default
        }.getOrDefault(default)
    }

    private fun playSound(soundUri: String?) {
        val uri = resolveSoundUri(soundUri) ?: return
        runCatching {
            RingtoneManager.getRingtone(context, uri)?.play()
        }
    }

    private fun resolveSoundUri(soundUri: String?): Uri? {
        if (!soundUri.isNullOrBlank()) {
            return runCatching { Uri.parse(soundUri) }.getOrNull()
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_goal_alerts),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_goal_alerts_desc)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "run_goal_alerts"
        const val NOTIFICATION_ID = 7022
    }
}
