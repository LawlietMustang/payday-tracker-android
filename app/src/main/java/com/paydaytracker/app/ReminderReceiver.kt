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
        if (intent.action == ACTION) deliverDue(context) else PaydayWidget.update(context)
        schedule(context)
    }
    companion object {
        const val ACTION = "com.paydaytracker.app.REMIND"
        fun notificationsAllowed(context: Context): Boolean {
            val manager = context.getSystemService(NotificationManager::class.java)
            return manager.areNotificationsEnabled() && manager.getNotificationChannel("reminders")?.importance != NotificationManager.IMPORTANCE_NONE
        }
        fun deliverDue(context: Context) {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val due = prefs.getLong("nextReminder", 0)
            if (!prefs.getBoolean("reminder", false) || due <= 0 || due > System.currentTimeMillis()) return
            // One claim per scheduled occurrence prevents duplicates after opening the app.
            if (prefs.getLong("lastReminder", 0) == due) return
            prefs.edit().putLong("lastReminder", due).apply()
            if (System.currentTimeMillis() - due < 86400000) post(context)
        }
        fun post(context: Context, test: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
        val de = prefs.getString("language", "de") != "en"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("reminders", if (de) "Erinnerungen" else "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        if (!notificationsAllowed(context)) return false
        val open = PendingIntent.getActivity(context, 221, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, "reminders")
            .setSmallIcon(R.drawable.app_icon).setContentTitle("Payday Tracker")
            .setContentText(if (test) { if (de) "Test erfolgreich: Erinnerungen können angezeigt werden." else "Test successful: reminders can be displayed." } else if (de) "Zeit, deine Arbeitsstunden einzutragen." else "Time to log your work hours.")
            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
        return try { manager.notify(if (test) 222 else 221, notification); true } catch (_: SecurityException) { false }
        }
        fun schedule(context: Context) {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val alarm = context.getSystemService(AlarmManager::class.java)
            val pending = PendingIntent.getBroadcast(context, 221, Intent(context, ReminderReceiver::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarm.cancel(pending)
            if (!prefs.getBoolean("reminder", false)) { prefs.edit().putLong("nextReminder", 0).apply(); return }
            val days = prefs.getInt("reminderDays", 62)
            if (days == 0) return
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, prefs.getInt("reminderHour", 20))
                set(Calendar.MINUTE, prefs.getInt("reminderMinute", 0))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            while (next.timeInMillis <= System.currentTimeMillis() || days and (1 shl (next.get(Calendar.DAY_OF_WEEK) - 1)) == 0) next.add(Calendar.DAY_OF_YEAR, 1)
            prefs.edit().putLong("nextReminder", next.timeInMillis).apply()
            try {
                if (android.os.Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
                else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
            } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending) }
        }
    }
}
