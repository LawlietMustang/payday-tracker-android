package com.paydaytracker.app

import android.content.Context
import com.paydaytracker.app.data.AppSettings
import com.paydaytracker.app.data.MonthSummary
import com.paydaytracker.app.data.Payslip
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.data.WageRepository
import com.paydaytracker.app.data.Workplace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.YearMonth

object NativeCoordinator {
    private var timerToken: String? = null
    fun syncTimer(context: Context, document: JSONObject) {
        val timer = document.optJSONObject("activeTimer")
        val token = timer?.toString() ?: "idle"
        if (timerToken == token) return
        timerToken = token
        val now = System.currentTimeMillis()
        val totals = timer?.let { com.paydaytracker.app.ui.parity.timerTotals(it, now) }
        val state = timer?.optString("state") ?: "idle"
        val elapsed = if (state == "break") totals?.second ?: 0L else totals?.first ?: 0L
        val language = document.optString("language", context.getSharedPreferences("device", 0).getString("language", "de"))
        context.getSharedPreferences("device", 0).edit().putString("timerState", state).putLong("timerBase", now-elapsed).putString("language", language).apply()
        if (timer == null) context.stopService(android.content.Intent(context, TimerNotificationService::class.java))
        else try { context.startForegroundService(android.content.Intent(context, TimerNotificationService::class.java).setAction(TimerNotificationService.ACTION_UPDATE).putExtra(TimerNotificationService.EXTRA_STATE,state).putExtra(TimerNotificationService.EXTRA_ELAPSED,elapsed).putExtra(TimerNotificationService.EXTRA_LANGUAGE,language)) } catch (_: IllegalStateException) { timerToken = null }
        PaydayWidget.update(context)
    }


    fun syncWidget(context: Context, summary: MonthSummary?, settings: AppSettings, timerState: String = "idle", timerBase: Long = 0L) {
        val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "de") ?: "de"

        if (summary != null) {
            val doc = JSONObject()
            doc.put("month", YearMonth.now().toString())
            doc.put("minutes", summary.workedMinutes.toDouble())
            doc.put("target", settings.target)
            prefs.edit().putString("widgetProgress", doc.toString()).apply()
        }
        PaydayWidget.update(context)
    }

    fun syncAutoBackup(context: Context, repository: WageRepository, scope: CoroutineScope) =
        scope.launch(Dispatchers.IO) {
            val autoBackup = AutoBackup.get(context)
            val json = repository.exportBackupJson()
            autoBackup.enqueue(json, AutoBackup.snapshotHash(json))
        }


    fun syncShiftReminders(context: Context, shifts: List<Shift>, workplaces: List<Workplace>, document: JSONObject = JSONObject()) {
        val doc = JSONObject()
        val config = document.optJSONObject("shiftReminders") ?: JSONObject()
        val lead = config.optLong("amount", 1) * if (config.optString("unit") == "days") 1440 else 60
        doc.put("enabled", config.optBoolean("enabled"))
        doc.put("language", document.optString("language", "de"))
        doc.put("leadMinutes", lead.coerceIn(1, 43200))
        val wpMap = workplaces.associate { it.id to it.name }
        val shiftArray = JSONArray()
        shifts.filter { it.status == "planned" }.forEach { sh ->
            val o = JSONObject()
            o.put("id", sh.id)
            o.put("date", sh.date)
            o.put("start", sh.start)
            o.put("workplace", wpMap[sh.workplaceId] ?: "")
            shiftArray.put(o)
        }
        doc.put("glanceShifts", shiftArray)
        val ids = config.optJSONArray("ids") ?: JSONArray()
        val selected = (0 until ids.length()).map { ids.optString(it) }.toSet()
        val rows = JSONArray()
        for (i in 0 until shiftArray.length()) { val row = shiftArray.getJSONObject(i)
            if (config.optString("scope", "all") == "all" || row.optString("id") in selected) rows.put(row)
        }
        doc.put("shifts", rows)
        ShiftReminders.replace(context, doc.toString())
    }

    fun syncPayslipReminder(context: Context, payslips: List<Payslip>, enabled: Boolean, shifts: List<Shift> = emptyList()) {
        val doc = JSONObject()
        doc.put("enabled", enabled)
        val lang = context.getSharedPreferences("device", Context.MODE_PRIVATE).getString("language", "de") ?: "de"
        doc.put("language", lang)
        val monthsArray = JSONArray()
        shifts.filter { it.status != "cancelled" }.groupBy { it.date.take(7) }.forEach { (month, records) ->
            monthsArray.put(JSONObject().put("month",month).put("missing",records.map{it.workplaceId}.distinct().any { workplace ->
                payslips.none { it.month == month && it.workplaceId == workplace }
            }))
        }
        doc.put("months", monthsArray)
        PayslipReminder.update(context, doc.toString())
    }
}
