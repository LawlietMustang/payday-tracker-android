package com.paydaytracker.app.widget

import com.paydaytracker.app.R
import com.paydaytracker.app.ui.MainActivity
import com.paydaytracker.app.reminders.ShiftReminders
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

open class PaydayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context)
    companion object {
        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, com.paydaytracker.app.PaydayWidget::class.java))
            manager.updateAppWidget(ids, views(context))
        }
        fun views(context: Context, now: Long = System.currentTimeMillis()): RemoteViews {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val de = prefs.getString("language", "de") != "en"
            val locked = prefs.getBoolean("lock", false)
            val state = prefs.getString("timerState", "idle")
            val active = state == "working" || state == "break"
            val views = RemoteViews(context.packageName, R.layout.payday_widget)
            views.setTextViewText(R.id.widget_status, if (locked) { if (de) "Zum Entsperren öffnen" else "Open to unlock" }
                else if (state == "working") { if (de) "Arbeitszeit läuft" else "Working" }
                else if (state == "break") { if (de) "Pause läuft" else "On break" }
                else idleProgress(context, now))
            val next = if (!locked && !active) nextLabel(context, now) else ""
            views.setTextViewText(R.id.widget_next, next)
            views.setViewVisibility(R.id.widget_next, if (next.isEmpty()) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widget_open, if (de) "WageTrack öffnen →" else "Open WageTrack →")
            views.setViewVisibility(R.id.widget_clock, if (active && !locked) View.VISIBLE else View.GONE)
            val elapsed = (System.currentTimeMillis() - prefs.getLong("timerBase", System.currentTimeMillis())).coerceAtLeast(0)
            views.setChronometer(R.id.widget_clock, SystemClock.elapsedRealtime() - elapsed, null, active && !locked)
            val intent = Intent(context, com.paydaytracker.app.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val open = PendingIntent.getActivity(context, 220, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }
        private fun idleProgress(context: Context, now: Long): String {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val de = prefs.getString("language", "de") != "en"
            val doc = try { JSONObject(prefs.getString("widgetProgress", "{}")!!) } catch (_: Exception) { JSONObject() }
            val month = YearMonth.from(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())).toString()
            val minutes = if (doc.optString("month") == month) doc.optDouble("minutes", 0.0).toInt().coerceAtLeast(0) else 0
            val done = "${minutes / 60} h" + if (minutes % 60 > 0) " ${minutes % 60} min" else ""
            val target = doc.optDouble("target", 0.0)
            val goal = java.text.NumberFormat.getNumberInstance(if (de) Locale.GERMANY else Locale.UK).apply { maximumFractionDigits = 1 }.format(target)
            return if (target > 0) "$done / $goal h" else "$done · " + if (de) "diesen Monat" else "this month"
        }
        private fun nextLabel(context: Context, now: Long): String {
            val start = ShiftReminders.nextGlance(context, now) ?: return ""
            val de = context.getSharedPreferences("device", Context.MODE_PRIVATE).getString("language", "de") != "en"
            val zone = ZoneId.systemDefault(); val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val date = Instant.ofEpochMilli(start).atZone(zone)
            val day = when(date.toLocalDate()) {
                today -> if (de) "Heute" else "Today"
                today.plusDays(1) -> if (de) "Morgen" else "Tomorrow"
                else -> date.format(DateTimeFormatter.ofPattern("EEE", if (de) Locale.GERMANY else Locale.UK))
            }
            return (if (de) "Nächste: " else "Next: ") + day + " " + date.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }
}
