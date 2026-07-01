package com.luckierdev.adfreenaline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RunForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START, ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Run active"
                val content = intent.getStringExtra(EXTRA_CONTENT) ?: "Tracking your run"
                val notification = buildNotification(title, content)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    private fun ensureChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Live Run", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "run_live_channel"
        const val NOTIFICATION_ID = 7021
        const val ACTION_START = "com.luckierdev.adfreenaline.action.START_RUN_SERVICE"
        const val ACTION_UPDATE = "com.luckierdev.adfreenaline.action.UPDATE_RUN_SERVICE"
        const val ACTION_STOP = "com.luckierdev.adfreenaline.action.STOP_RUN_SERVICE"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
    }
}
