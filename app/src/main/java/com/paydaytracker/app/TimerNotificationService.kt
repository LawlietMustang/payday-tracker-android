package com.paydaytracker.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock

class TimerNotificationService : Service() {
    companion object {
        const val ACTION_UPDATE = "com.paydaytracker.app.TIMER_UPDATE"
        const val ACTION_STOP = "com.paydaytracker.app.TIMER_STOP"
        const val EXTRA_STATE = "state"
        const val EXTRA_ELAPSED = "elapsed"
        const val EXTRA_LANGUAGE = "language"
        private const val CHANNEL_ID = "payday_active_timer"
        private const val NOTIFICATION_ID = 1801
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Active work timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the running Payday Tracker work or break timer"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val state = intent?.getStringExtra(EXTRA_STATE) ?: "working"
        val elapsed = intent?.getLongExtra(EXTRA_ELAPSED, 0L) ?: 0L
        val english = intent?.getStringExtra(EXTRA_LANGUAGE) == "en"
        val onBreak = state == "break"
        val status = if (english) { if (onBreak) "On break" else "Working" } else { if (onBreak) "Pause läuft" else "Arbeitszeit läuft" }
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Payday Tracker")
            .setContentText(status)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            .setWhen(System.currentTimeMillis() - elapsed)
            .setUsesChronometer(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
