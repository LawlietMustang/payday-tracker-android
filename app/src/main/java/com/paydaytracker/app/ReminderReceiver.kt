package com.paydaytracker.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        schedule(context)
        if (intent.action != ACTION) { PaydayWidget.update(context); return }
        val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder", false)) return
        val de = prefs.getString("language", "de") != "en"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("reminders", if (de) "Erinnerungen" else "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        if (!manager.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(context, 221, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, "reminders")
            .setSmallIcon(R.drawable.app_icon).setContentTitle("Payday Tracker")
            .setContentText(if (de) "Zeit, deine Arbeitsstunden einzutragen." else "Time to log your work hours.")
            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
        try { manager.notify(221, notification) } catch (_: SecurityException) { }
    }
    companion object {
        const val ACTION = "com.paydaytracker.app.REMIND"
        fun schedule(context: Context) {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val alarm = context.getSystemService(AlarmManager::class.java)
            val pending = PendingIntent.getBroadcast(context, 221, Intent(context, ReminderReceiver::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarm.cancel(pending)
            if (!prefs.getBoolean("reminder", false)) return
            val days = prefs.getInt("reminderDays", 62)
            if (days == 0) return
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, prefs.getInt("reminderHour", 20))
                set(Calendar.MINUTE, prefs.getInt("reminderMinute", 0))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            while (next.timeInMillis <= System.currentTimeMillis() || days and (1 shl (next.get(Calendar.DAY_OF_WEEK) - 1)) == 0) next.add(Calendar.DAY_OF_YEAR, 1)
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
        }
    }
}
