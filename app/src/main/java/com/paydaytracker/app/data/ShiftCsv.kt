package com.paydaytracker.app.data

import java.util.Locale

/** The same per-shift pay detail as the original export, in the selected currency. */
object ShiftCsv {
    fun export(month: String, shifts: List<Shift>, settings: AppSettings, workplaces: List<Workplace>, filter: String = "all"): String {
        val sum = PayrollCalculator.summary(month, shifts, settings, workplaces, filter)
        val currency = settings.currency
        val header = "Date,Workplace,Start,End,Break_minutes,Working_minutes,Base_gross_$currency,Overtime_bonus_$currency,Night_bonus_$currency,Sunday_bonus_$currency,Holiday_bonus_$currency,Total_gross_$currency,Status,Cancelled_by,Note"
        fun money(value: Double) = String.format(Locale.ROOT, "%.2f", value)
        fun cell(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
        val rows = shifts.filter { it.date.startsWith(month) && (filter == "all" || it.workplaceId == filter) }.sortedBy { it.date + it.start }.map { shift ->
            val workplace = workplaces.firstOrNull { it.id == shift.workplaceId }
            val resolved = shift.copy(wage = shift.wage ?: workplace?.wage ?: settings.wage)
            val bonus = sum.bonusById[shift.id] ?: PayrollCalculator.bonusForEntry(resolved, settings)
            val base = PayrollCalculator.grossEntry(resolved, settings.wage)
            listOf(shift.date, workplace?.name ?: "", shift.start, shift.end, shift.breakMin.toString(),
                (if (shift.status == "cancelled") 0 else shift.minutes).toString(), money(base), money(bonus.overtime), money(bonus.night), money(bonus.sunday), money(bonus.holiday), money(base + bonus.total), shift.status,
                if (shift.status == "cancelled") shift.cancelledBy ?: "unspecified" else "", shift.note ?: "").joinToString(",", transform = ::cell)
        }
        return "\uFEFF" + (listOf(header) + rows).joinToString("\r\n")
    }
}
