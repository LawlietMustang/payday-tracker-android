package com.paydaytracker.app.reminders

import com.paydaytracker.app.ui.MainActivity
import com.paydaytracker.app.R
import android.app.*
import android.content.*
import org.json.JSONObject
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

// One optional local alarm. No account, network or per-edit notifications.
open class PayslipReminder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { reconcile(context) }
    companion object {
        const val ID = 227
        const val CHANNEL = "payslips"
        const val EXTRA_MONTH = "payslip_month"
        private const val DAY = 86400000L
        private fun prefs(c: Context) = c.getSharedPreferences("payslip_reminder", Context.MODE_PRIVATE)
        private fun pending(c: Context) = PendingIntent.getBroadcast(c, ID, Intent(c, com.paydaytracker.app.PayslipReminder::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private fun monthAt(now: Long) = YearMonth.from(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()))
        private fun seventh(month: YearMonth) = month.atDay(7).atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        @Synchronized fun update(c: Context, raw: String, now: Long = System.currentTimeMillis()) {
            if (raw.length > 200000) return
            val config = try {
                JSONObject(raw).also { doc ->
                    val months = doc.getJSONArray("months"); require(months.length() <= 1200)
                    for (i in 0 until months.length()) YearMonth.parse(months.getJSONObject(i).getString("month"))
                }
            } catch (_: Exception) { return }
            prefs(c).edit().putString("config", config.toString()).commit()
            reconcile(c, now, true)
        }
        @Synchronized fun reconcile(c: Context, now: Long = System.currentTimeMillis(), fromEdit: Boolean = false) {
            val p = prefs(c); val alarm = c.getSystemService(AlarmManager::class.java)
            val manager = c.getSystemService(NotificationManager::class.java); alarm.cancel(pending(c))
            val config = try { JSONObject(p.getString("config", "{}")!!) } catch (_: Exception) { JSONObject() }
            if (!config.optBoolean("enabled")) {
                p.edit().remove("month").remove("due").commit(); manager.cancel(ID); return
            }
            val current = monthAt(now); val previous = current.minusMonths(1).toString()
            val months = config.optJSONArray("months")
            val missing = months != null && (0 until months.length()).any {
                val row = months.getJSONObject(it); row.optString("month") == previous && row.optBoolean("missing")
            }
            var due = seventh(current.plusMonths(1))
            if (missing) {
                due = if (p.getString("month", "") == previous) p.getLong("due", seventh(current))
                    else maxOf(seventh(current), if (fromEdit) now + 2 * DAY else 0)
                if (due <= now) {
                    val de = config.optString("language", "de") != "en"
                    manager.createNotificationChannel(NotificationChannel(CHANNEL, if (de) "Lohnabrechnungen" else "Payslips", NotificationManager.IMPORTANCE_DEFAULT))
                    if (manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE) {
                        val intent = Intent(c, com.paydaytracker.app.MainActivity::class.java).putExtra(EXTRA_MONTH, previous).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        val open = PendingIntent.getActivity(c, ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        val text = if (de) "Trage deine letzte Lohnabrechnung ein und vergleiche deinen Lohn." else "Enter your last payslip to compare your pay."
                        val notice = Notification.Builder(c, CHANNEL).setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle(if (de) "Lohnabrechnung eintragen" else "Enter payslip").setContentText(text)
                            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
                        try { manager.notify(ID, notice) } catch (_: SecurityException) { }
                    }
                    // Denial/channel blocking also advances cadence: never spam on resume.
                    due = now + 7 * DAY
                }
                p.edit().putString("month", previous).putLong("due", due).commit()
            } else {
                p.edit().remove("month").remove("due").commit(); manager.cancel(ID)
            }
            // Re-evaluate at the next month's seventh even if a weekly retry falls later.
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, minOf(due, seventh(current.plusMonths(1))).coerceAtLeast(now + 1000), pending(c))
        }
    }
}
