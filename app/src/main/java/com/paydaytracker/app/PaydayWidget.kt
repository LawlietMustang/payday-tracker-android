package com.paydaytracker.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews

class PaydayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context)
    companion object {
        fun update(context: Context) {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PaydayWidget::class.java))
            val de = prefs.getString("language", "de") != "en"
            val locked = prefs.getBoolean("lock", false)
            val state = prefs.getString("timerState", "idle")
            val active = state == "working" || state == "break"
            val views = RemoteViews(context.packageName, R.layout.payday_widget)
            views.setTextViewText(R.id.widget_status, if (locked) { if (de) "Zum Entsperren öffnen" else "Open to unlock" }
                else if (state == "working") { if (de) "Arbeitszeit läuft" else "Working" }
                else if (state == "break") { if (de) "Pause läuft" else "On break" }
                else { if (de) "Bereit zum Start" else "Ready to start" })
            views.setTextViewText(R.id.widget_open, if (de) "Payday Tracker öffnen →" else "Open Payday Tracker →")
            views.setViewVisibility(R.id.widget_clock, if (active && !locked) View.VISIBLE else View.GONE)
            val elapsed = (System.currentTimeMillis() - prefs.getLong("timerBase", System.currentTimeMillis())).coerceAtLeast(0)
            views.setChronometer(R.id.widget_clock, SystemClock.elapsedRealtime() - elapsed, null, active && !locked)
            val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val open = PendingIntent.getActivity(context, 220, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            manager.updateAppWidget(ids, views)
        }
    }
}
