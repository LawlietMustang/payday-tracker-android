package com.paydaytracker.app.reminders

import com.paydaytracker.app.R

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

open class BackupReminder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (prefs.getBoolean("backupDirty", false) && prefs.getLong("backupDue", Long.MAX_VALUE) <= now) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val de = prefs.getString("language", "de") != "en"
            manager.createNotificationChannel(NotificationChannel("backups", if (de) "Sicherungen" else "Backups", NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(context, 226, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(context, "backups").setSmallIcon(R.drawable.notification_icon).setContentTitle("WageTrack")
                .setContentText(if (de) "Deine Änderungen sind noch nicht gesichert. Speichere eine Sicherungsdatei." else "Your changes are not backed up yet. Save a backup file.")
                .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
            if (manager.areNotificationsEnabled()) try { manager.notify(226, notification) } catch (_: SecurityException) { }
            // Even if permission is blocked, do not repeatedly nudge on every app opening.
            prefs.edit().putLong("backupDue", now + WEEK).apply()
        }
        schedule(context)
    }
    companion object {
        private const val WEEK = 7L * 24 * 60 * 60 * 1000
        fun update(context: Context, dirty: Boolean, language: String) {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val edit = prefs.edit().putBoolean("backupDirty", dirty).putString("language", if (language == "en") "en" else "de")
            if (dirty && !prefs.getBoolean("backupDirty", false)) edit.putLong("backupDue", System.currentTimeMillis() + 2L * 24 * 60 * 60 * 1000)
            if (!dirty) edit.remove("backupDue")
            edit.apply(); schedule(context)
        }
        fun schedule(context: Context) {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val alarm = context.getSystemService(AlarmManager::class.java)
            val pending = PendingIntent.getBroadcast(context, 226, Intent(context, com.paydaytracker.app.BackupReminder::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarm.cancel(pending)
            if (!prefs.getBoolean("backupDirty", false)) { context.getSystemService(NotificationManager::class.java).cancel(226); return }
            val due = prefs.getLong("backupDue", System.currentTimeMillis() + WEEK)
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due.coerceAtLeast(System.currentTimeMillis() + 1000), pending)
        }
    }
}
