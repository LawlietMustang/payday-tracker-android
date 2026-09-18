package com.paydaytracker.app

import android.app.*
import android.content.*
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ShiftReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { ShiftReminders.reconcile(context) }
}

// A single native alarm points to the next reminder; no WebView or network is needed.
object ShiftReminders {
    private const val CHANNEL = "upcoming_shifts"
    private fun prefs(c: Context) = c.getSharedPreferences("shift_reminders", Context.MODE_PRIVATE)
    private fun pending(c: Context) = PendingIntent.getBroadcast(c, 225, Intent(c, ShiftReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun startMillis(row: JSONObject): Long = try {
        LocalDateTime.parse(row.getString("date") + "T" + row.getString("start")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) { 0L }
    private fun key(row: JSONObject, lead: Long) = row.getString("id") + ":" + row.getString("date") + ":" + row.getString("start") + ":" + lead
    @Synchronized fun replace(c: Context, raw: String): Boolean {
        if (raw.length > 2000000) return false
        return try {
            val doc = JSONObject(raw); val lead = doc.getLong("leadMinutes"); val rows = doc.getJSONArray("shifts")
            require(lead in 1..43200 && rows.length() <= 10000)
            val ids = mutableSetOf<String>()
            for (i in 0 until rows.length()) {
                val r = rows.getJSONObject(i); val id = r.getString("id")
                require(id.isNotBlank() && id.length <= 200 && ids.add(id) && startMillis(r) > 0)
                require(r.optString("workplace").length <= 160)
            }
            prefs(c).edit().putString("config", doc.toString()).commit()
            reconcile(c); true
        } catch (_: Exception) { false }
    }
    fun status(c: Context): JSONObject {
        val p = prefs(c); val manager = c.getSystemService(NotificationManager::class.java)
        return JSONObject().put("next", p.getLong("next", 0)).put("count", p.getInt("count", 0))
            .put("allowed", manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
    }
    @Synchronized fun reconcile(c: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(c); val alarm = c.getSystemService(AlarmManager::class.java); alarm.cancel(pending(c))
        val config = try { JSONObject(p.getString("config", "{}")!!) } catch (_: Exception) { JSONObject() }
        val rows = config.optJSONArray("shifts") ?: JSONArray(); val enabled = config.optBoolean("enabled")
        val lead = config.optLong("leadMinutes", 60).coerceIn(1, 43200)
        val sent = try { JSONObject(p.getString("sent", "{}")!!) } catch (_: Exception) { JSONObject() }
        val keep = JSONObject(); var next = Long.MAX_VALUE; var count = 0
        val activeKeys = mutableSetOf<String>()
        if (enabled) for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i); val start = startMillis(row)
            if (start <= now) continue
            val k = key(row, lead); activeKeys.add(k)
            if (sent.optBoolean(k)) { keep.put(k, true); continue }
            val due = start - lead * 60000
            if (due <= now) {
                post(c, row, k, start, config.optString("language", "de"))
                // Claim once even if permission is blocked; avoid a repeating alarm loop.
                keep.put(k, true)
            } else { count++; next = minOf(next, due) }
        }
        val notifications = c.getSystemService(NotificationManager::class.java)
        notifications.activeNotifications.filter { it.id == 225 && it.tag !in activeKeys }.forEach { notifications.cancel(it.tag, 225) }
        p.edit().putString("sent", keep.toString()).putInt("count", count).putLong("next", if (next == Long.MAX_VALUE) 0 else next).apply()
        if (next == Long.MAX_VALUE) return
        try {
            if (Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(c))
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(c))
        } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(c)) }
    }
    private fun post(c: Context, row: JSONObject, key: String, start: Long, language: String) {
        val de = language != "en"; val manager = c.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, if (de) "Bevorstehende Schichten" else "Upcoming shifts", NotificationManager.IMPORTANCE_DEFAULT))
        if (!status(c).getBoolean("allowed")) return
        val locked = c.getSharedPreferences("device", Context.MODE_PRIVATE).getBoolean("lock", false)
        val time = java.time.Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, dd MMM · HH:mm", if (de) Locale.GERMAN else Locale.UK))
        val text = if (locked) { if (de) "Öffne die App für deine bevorstehende Schicht." else "Open the app to view your upcoming shift." }
                   else row.optString("workplace") + " · " + time
        val open = PendingIntent.getActivity(c, 225, Intent(c, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(c, CHANNEL).setSmallIcon(R.drawable.app_icon)
            .setContentTitle(if (de) "Bevorstehende Schicht" else "Upcoming shift").setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).build()
        try { manager.notify(key, 225, notification) } catch (_: SecurityException) { }
    }
}
