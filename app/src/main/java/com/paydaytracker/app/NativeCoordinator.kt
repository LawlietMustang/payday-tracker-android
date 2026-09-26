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
import java.security.MessageDigest
import java.time.YearMonth

object NativeCoordinator {

    fun syncWidget(context: Context, summary: MonthSummary?, settings: AppSettings, timerState: String = "idle", timerBase: Long = 0L) {
        val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "de") ?: "de"
        prefs.edit()
            .putString("timerState", timerState)
            .putLong("timerBase", timerBase)
            .putString("language", language)
            .apply()

        if (summary != null) {
            val doc = JSONObject()
            doc.put("month", YearMonth.now().toString())
            doc.put("minutes", summary.workedMinutes.toDouble())
            doc.put("target", settings.target)
            prefs.edit().putString("widgetProgress", doc.toString()).apply()
        }
        PaydayWidget.update(context)
    }

    fun syncAutoBackup(context: Context, repository: WageRepository, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val autoBackup = AutoBackup.get(context)
            val json = repository.exportBackupJson()
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(json.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            autoBackup.enqueue(json, hash)
            val lang = context.getSharedPreferences("device", Context.MODE_PRIVATE).getString("language", "de") ?: "de"
            BackupReminder.update(context, dirty = true, language = lang)
        }
    }

    fun syncShiftReminders(context: Context, shifts: List<Shift>, workplaces: List<Workplace>, leadMinutes: Long = 60) {
        val doc = JSONObject()
        doc.put("leadMinutes", leadMinutes)
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
        doc.put("shifts", shiftArray)
        ShiftReminders.replace(context, doc.toString())
    }

    fun syncPayslipReminder(context: Context, payslips: List<Payslip>, enabled: Boolean) {
        val doc = JSONObject()
        doc.put("enabled", enabled)
        val lang = context.getSharedPreferences("device", Context.MODE_PRIVATE).getString("language", "de") ?: "de"
        doc.put("language", lang)
        val current = YearMonth.now()
        val prev = current.minusMonths(1).toString()
        val hasPrev = payslips.any { it.month == prev }
        val monthsArray = JSONArray()
        monthsArray.put(JSONObject().apply {
            put("month", prev)
            put("missing", !hasPrev)
        })
        doc.put("months", monthsArray)
        PayslipReminder.update(context, doc.toString())
    }
}
