package com.paydaytracker.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

data class ShiftInterval(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val ratio: Double
)

data class ShiftBonus(
    val overtime: Double,
    val night: Double,
    val sunday: Double,
    val holiday: Double,
    val total: Double,
    val overtimeMinutes: Double,
    val nightMinutes: Double,
    val sundayMinutes: Double,
    val holidayMinutes: Double
)

data class TaxEstimate(
    val wageTax: Double,
    val soli: Double,
    val church: Double,
    val pension: Double,
    val unemployment: Double,
    val health: Double,
    val care: Double,
    val manualDeduction: Double?,
    val net: Double
)

data class BonusBreakdown(
    var overtime: Double = 0.0,
    var night: Double = 0.0,
    var sunday: Double = 0.0,
    var holiday: Double = 0.0,
    var total: Double = 0.0
)

data class MonthSummary(
    val list: List<Shift>,
    val done: List<Shift>,
    val completed: List<Shift>,
    val workedMinutes: Int,
    val minutes: Int,
    val baseGross: Double,
    val bonus: BonusBreakdown,
    val bonusById: Map<String, ShiftBonus>,
    val gross: Double,
    val est: TaxEstimate
)

data class MonthForecast(
    val minutes: Int,
    val gross: Double,
    val confidence: String
)

data class SpendingProjection(
    val spent: Double,
    val scheduled: Double,
    val projected: Double,
    val logged: Double,
    val past: Boolean,
    val future: Boolean,
    val hasData: Boolean
)

data class ReconcileResult(
    val updatedGoals: List<SavingsGoal>,
    val newLedgerEntries: List<SavingsLedgerEntry>,
    val changed: Boolean
)

object PayrollCalculator {

    fun easterDate(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * (b % 4) + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    fun holidays(year: Int, state: String): Set<String> {
        val e = easterDate(year)
        val set = mutableSetOf(
            String.format("%04d-01-01", year),
            String.format("%04d-05-01", year),
            String.format("%04d-10-03", year),
            String.format("%04d-12-25", year),
            String.format("%04d-12-26", year)
        )
        listOf(-2L, 1L, 39L, 50L).forEach { n ->
            set.add(e.plusDays(n).toString())
        }
        if (state in listOf("Baden-Württemberg", "Bayern", "Sachsen-Anhalt")) {
            set.add(String.format("%04d-01-06", year))
        }
        if (state in listOf("Berlin", "Mecklenburg-Vorpommern")) {
            set.add(String.format("%04d-03-08", year))
        }
        if (state == "Brandenburg") {
            set.add(e.toString())
            set.add(e.plusDays(49).toString())
        }
        if (state in listOf("Baden-Württemberg", "Bayern", "Hessen", "Nordrhein-Westfalen", "Rheinland-Pfalz", "Saarland")) {
            set.add(e.plusDays(60).toString())
        }
        if (state == "Saarland") {
            set.add(String.format("%04d-08-15", year))
        }
        if (state == "Thüringen") {
            set.add(String.format("%04d-09-20", year))
        }
        if (state in listOf("Brandenburg", "Bremen", "Hamburg", "Mecklenburg-Vorpommern", "Niedersachsen", "Sachsen", "Sachsen-Anhalt", "Schleswig-Holstein", "Thüringen")) {
            set.add(String.format("%04d-10-31", year))
        }
        if (state in listOf("Baden-Württemberg", "Bayern", "Nordrhein-Westfalen", "Rheinland-Pfalz", "Saarland")) {
            set.add(String.format("%04d-11-01", year))
        }
        if (state == "Sachsen") {
            var d = LocalDate.of(year, 11, 23)
            while (d.dayOfWeek != DayOfWeek.WEDNESDAY) {
                d = d.minusDays(1)
            }
            set.add(d.toString())
        }
        return set
    }

    fun shiftRange(shift: Shift): ShiftInterval {
        val startDate = LocalDate.parse(shift.date)
        val startTime = LocalTime.parse(shift.start)
        val endTime = LocalTime.parse(shift.end)
        val start = LocalDateTime.of(startDate, startTime)
        var end = LocalDateTime.of(startDate, endTime)
        if (!end.isAfter(start)) {
            end = end.plusDays(1)
        }
        val spanMinutes = java.time.Duration.between(start, end).toMinutes().toDouble()
        val ratio = shift.minutes / maxOf(1.0, spanMinutes)
        return ShiftInterval(start, end, ratio)
    }

    fun overlapMinutes(a: LocalDateTime, b: LocalDateTime, c: LocalDateTime, d: LocalDateTime): Double {
        val start = if (a.isAfter(c)) a else c
        val end = if (b.isBefore(d)) b else d
        val dur = java.time.Duration.between(start, end).toMinutes()
        return if (dur > 0) dur.toDouble() else 0.0
    }

    fun specialMinutes(shift: Shift, type: String, settings: AppSettings): Double {
        if (type == "holiday" && (settings.currency != "EUR" || settings.taxMode == "manual")) return 0.0
        val r = shiftRange(shift)
        var sum = 0.0
        var cursorDate = r.start.toLocalDate()
        while (cursorDate.atStartOfDay().isBefore(r.end)) {
            val cursorStart = cursorDate.atStartOfDay()
            val cursorEnd = cursorDate.plusDays(1).atStartOfDay()
            val qualifies = if (type == "sunday") {
                cursorDate.dayOfWeek == DayOfWeek.SUNDAY
            } else {
                holidays(cursorDate.year, settings.state).contains(cursorDate.toString())
            }
            if (qualifies) {
                sum += overlapMinutes(r.start, r.end, cursorStart, cursorEnd)
            }
            cursorDate = cursorDate.plusDays(1)
        }
        return sum * r.ratio
    }

    fun nightMinutes(shift: Shift, settings: AppSettings): Double {
        val r = shiftRange(shift)
        fun toMin(s: String): Int {
            val p = s.split(":").map { it.toInt() }
            return p[0] * 60 + p[1]
        }
        val ns = toMin(if (settings.nightStart.isNotEmpty()) settings.nightStart else "22:00")
        val ne = toMin(if (settings.nightEnd.isNotEmpty()) settings.nightEnd else "06:00")
        var sum = 0.0
        val baseDate = r.start.toLocalDate().minusDays(1)
        for (i in 0 until 4) {
            val d = baseDate.plusDays(i.toLong())
            val a = d.atStartOfDay().plusMinutes(ns.toLong())
            val b = d.atStartOfDay().plusMinutes((ne + if (ne <= ns) 1440 else 0).toLong())
            sum += overlapMinutes(r.start, r.end, a, b)
        }
        return minOf(shift.minutes.toDouble(), sum * r.ratio)
    }

    fun bonusForEntry(shift: Shift, settings: AppSettings, overtimeMinutes: Int = 0): ShiftBonus {
        if (shift.status == "cancelled") {
            return ShiftBonus(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        val wage = shift.wage ?: settings.wage
        val unit = wage / 60.0
        val night = nightMinutes(shift, settings)
        val sunday = specialMinutes(shift, "sunday", settings)
        val holiday = specialMinutes(shift, "holiday", settings)
        val ot = unit * overtimeMinutes * (settings.overtimeRate / 100.0)
        val n = unit * night * (settings.nightRate / 100.0)
        val s = unit * sunday * (settings.sundayRate / 100.0)
        val h = unit * holiday * (settings.holidayRate / 100.0)
        val total = ot + n + s + h
        return ShiftBonus(ot, n, s, h, total, overtimeMinutes.toDouble(), night, sunday, holiday)
    }

    fun grossEntry(shift: Shift, fallbackWage: Double): Double {
        if (shift.status == "cancelled") return 0.0
        val w = shift.wage ?: fallbackWage
        return shift.minutes * w / 60.0
    }

    fun estimate(gross: Double, settings: AppSettings): TaxEstimate {
        if (settings.currency != "EUR" || settings.taxMode == "manual") {
            val pct = maxOf(0.0, minOf(100.0, settings.deductionPercent)) / 100.0
            val deduction = gross * pct
            return TaxEstimate(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, deduction, maxOf(0.0, gross - deduction))
        }
        val annual = gross * 12.0
        var allowance = 12348.0
        if (settings.taxclass == "II") allowance += 4260.0
        if (settings.taxclass == "III") allowance *= 2.0
        if (settings.taxclass == "V" || settings.taxclass == "VI") allowance = 0.0
        val taxable = maxOf(0.0, annual - allowance - 1230.0)
        var annualTax = 0.0
        if (taxable > 0.0) {
            val rate = minOf(0.42, 0.14 + (taxable / 70000.0) * 0.28)
            annualTax = taxable * rate
            if (taxable > 277825.0) {
                annualTax += (taxable - 277825.0) * 0.03
            }
        }
        val wageTax = maxOf(0.0, annualTax / 12.0)
        val soli = if (wageTax > 1500.0) wageTax * 0.055 else 0.0
        val churchRate = if (settings.state == "Bayern" || settings.state == "Baden-Württemberg") 0.08 else 0.09
        val church = if (settings.church) wageTax * churchRate else 0.0
        val social = minOf(gross, 8050.0)
        val healthBase = minOf(gross, 5812.5)
        val pension = social * 0.093
        val unemployment = social * 0.013
        val health = healthBase * (0.073 + settings.health / 200.0)
        val care = healthBase * if (settings.childless) 0.024 else 0.018
        val net = maxOf(0.0, gross - wageTax - soli - church - pension - unemployment - health - care)
        return TaxEstimate(wageTax, soli, church, pension, unemployment, health, care, null, net)
    }

    fun summary(
        monthKey: String,
        shifts: List<Shift>,
        settings: AppSettings,
        workplaces: List<Workplace> = emptyList(),
        filterWorkplaceId: String = "all",
        cutoff: LocalDateTime? = null,
        includePlanned: Boolean = true
    ): MonthSummary {
        val workplaceMap = workplaces.associateBy { it.id }
        val list = shifts.filter { it.date.startsWith(monthKey) && (filterWorkplaceId == "all" || it.workplaceId == filterWorkplaceId) }
            .sortedWith(compareByDescending<Shift> { it.date }.thenByDescending { it.created })

        val done = list.filter { shift ->
            val statusMatches = if (includePlanned) shift.status != "cancelled" else shift.status == "completed"
            val cutoffMatches = cutoff == null || !shiftRange(shift).end.isAfter(cutoff)
            statusMatches && cutoffMatches
        }.sortedWith(compareBy<Shift> { it.date + it.start })

        var minutes = 0
        var baseGross = 0.0
        val bonus = BonusBreakdown()
        val bonusById = mutableMapOf<String, ShiftBonus>()
        val workedByPlace = mutableMapOf<String, Int>()
        val threshold = (settings.overtimeAfter * 60).toInt()

        done.forEach { e ->
            val wid = if (e.workplaceId.isNotEmpty()) e.workplaceId else "default"
            val worked = workedByPlace[wid] ?: 0
            val before = maxOf(0, worked - threshold)
            val after = maxOf(0, worked + e.minutes - threshold)
            val b = bonusForEntry(e, settings, after - before)
            workedByPlace[wid] = worked + e.minutes
            bonusById[e.id] = b
            minutes += e.minutes
            val fallbackWage = workplaceMap[e.workplaceId]?.wage ?: settings.wage
            baseGross += grossEntry(e, fallbackWage)
            bonus.overtime += b.overtime
            bonus.night += b.night
            bonus.sunday += b.sunday
            bonus.holiday += b.holiday
            bonus.total += b.total
        }

        val gross = baseGross + bonus.total
        val completedShifts = done.filter { it.status == "completed" }
        val workedMinutes = completedShifts.sumOf { it.minutes }

        return MonthSummary(
            list = list,
            done = done,
            completed = completedShifts,
            workedMinutes = workedMinutes,
            minutes = minutes,
            baseGross = baseGross,
            bonus = bonus,
            bonusById = bonusById,
            gross = gross,
            est = estimate(gross, settings)
        )
    }

    fun forecast(
        s: MonthSummary,
        settings: AppSettings,
        selected: String,
        now: LocalDate = LocalDate.now()
    ): MonthForecast {
        val currentMonthKey = String.format("%04d-%02d", now.year, now.monthValue)
        if (selected != currentMonthKey || s.done.isEmpty()) {
            return MonthForecast(s.minutes, s.gross, "Keine Daten")
        }
        val occupied = s.list.map { it.date }.toSet()
        val parts = selected.split("-").map { it.toInt() }
        val y = parts[0]
        val m = parts[1]
        val lastDay = YearMonth.of(y, m).lengthOfMonth()
        var unused = 0
        for (d in (now.dayOfMonth + 1)..lastDay) {
            val date = LocalDate.of(y, m, d)
            val iso = String.format("%04d-%02d-%02d", y, m, d)
            // JavaScript getDay(): 0=Sun, 1=Mon, ..., 6=Sat
            val jsDayOfWeek = if (date.dayOfWeek == DayOfWeek.SUNDAY) 0 else date.dayOfWeek.value
            if (settings.days.contains(jsDayOfWeek) && !occupied.contains(iso)) {
                unused++
            }
        }
        val avg = s.minutes.toDouble() / s.done.size
        val avgGross = s.gross / s.done.size
        val confidence = when {
            s.completed.size >= 8 -> "Hoch"
            s.completed.size >= 3 -> "Mittel"
            else -> "Niedrig"
        }
        return MonthForecast(
            minutes = (s.minutes + unused * avg).toInt(),
            gross = s.gross + unused * avgGross,
            confidence = confidence
        )
    }

    fun spendingProjection(
        monthKey: String,
        expenses: List<Expense>,
        now: LocalDate = LocalDate.now()
    ): SpendingProjection {
        val current = String.format("%04d-%02d", now.year, now.monthValue)
        val list = expenses.filter { it.date.startsWith(monthKey) }
        val parts = monthKey.split("-").map { it.toInt() }
        val daysInMonth = YearMonth.of(parts[0], parts[1]).lengthOfMonth()
        val today = now.toString()
        val past = monthKey < current
        val future = monthKey > current
        val logged = list.sumOf { it.amount }
        val incurred = list.filter { past || (!future && it.date <= today) }
        val spent = incurred.sumOf { it.amount }
        val variable = incurred.filter { it.recurringId == null }.sumOf { it.amount }
        val scheduled = logged - spent
        val projected = when {
            past -> logged
            future -> logged
            else -> spent + scheduled + (variable / now.dayOfMonth) * (daysInMonth - now.dayOfMonth)
        }
        return SpendingProjection(spent, scheduled, projected, logged, past, future, list.isNotEmpty())
    }

    fun cents(n: Double): Long = Math.round(n * 100.0)

    fun nextSavingsDate(dateStr: String, interval: String, anchor: Int? = null): String {
        val d = LocalDate.parse(dateStr)
        val day = anchor ?: d.dayOfMonth
        val nextDate = when (interval) {
            "weekly" -> d.plusWeeks(1)
            "yearly" -> {
                val targetYearMonth = YearMonth.of(d.year + 1, d.month)
                val targetDay = minOf(day, targetYearMonth.lengthOfMonth())
                LocalDate.of(targetYearMonth.year, targetYearMonth.month, targetDay)
            }
            else -> { // "monthly"
                val targetYearMonth = YearMonth.of(d.year, d.month).plusMonths(1)
                val targetDay = minOf(day, targetYearMonth.lengthOfMonth())
                LocalDate.of(targetYearMonth.year, targetYearMonth.month, targetDay)
            }
        }
        return nextDate.toString()
    }

    fun savingsAvailable(
        now: LocalDateTime,
        shifts: List<Shift>,
        expenses: List<Expense>,
        recurringExpenses: List<RecurringExpense>,
        savingsGoals: List<SavingsGoal>,
        savingsLedger: List<SavingsLedgerEntry>,
        settings: AppSettings,
        workplaces: List<Workplace> = emptyList()
    ): Long {
        val today = now.toLocalDate().toString()
        val nowMonthKey = String.format("%04d-%02d", now.year, now.monthValue)

        val months = shifts.filter { it.status == "completed" && !shiftRange(it).end.isAfter(now) }
            .map { it.date.substring(0, 7) }
            .distinct()

        var incomeCents = 0L
        for (m in months) {
            val s = summary(m, shifts, settings, workplaces, filterWorkplaceId = "all", cutoff = now, includePlanned = false)
            incomeCents += cents(s.est.net)
        }

        var expensesCents = expenses.filter { it.date <= today }.sumOf { cents(it.amount) }

        for (t in recurringExpenses) {
            var key = t.startMonth
            var i = 0
            while (key <= nowMonthKey && i < 1200) {
                val parts = key.split("-").map { it.toInt() }
                val y = parts[0]
                val m = parts[1]
                val maxDay = YearMonth.of(y, m).lengthOfMonth()
                val day = minOf(t.day, maxDay)
                val dateStr = String.format("%04d-%02d-%02d", y, m, day)
                val hasExplicit = expenses.any { it.recurringId == t.id && it.date.startsWith(key) }
                if (dateStr <= today && !hasExplicit) {
                    expensesCents += cents(t.amount)
                }
                val nextMonth = YearMonth.of(y, m).plusMonths(1)
                key = String.format("%04d-%02d", nextMonth.year, nextMonth.monthValue)
                i++
            }
        }

        val allocatedCents = savingsLedger.sumOf { cents(it.amount) }
        val manualCents = savingsGoals.sumOf { g ->
            val ledgerForGoal = savingsLedger.filter { it.goalId == g.id }.sumOf { cents(it.amount) }
            maxOf(0L, cents(g.saved) - ledgerForGoal)
        }

        return incomeCents - expensesCents - allocatedCents - manualCents
    }

    fun reconcileSavings(
        now: LocalDateTime,
        shifts: List<Shift>,
        expenses: List<Expense>,
        recurringExpenses: List<RecurringExpense>,
        goals: List<SavingsGoal>,
        ledger: List<SavingsLedgerEntry>,
        settings: AppSettings,
        workplaces: List<Workplace> = emptyList()
    ): ReconcileResult {
        val today = now.toLocalDate().toString()
        val currentLedger = ledger.toMutableList()
        val newLedgerEntries = mutableListOf<SavingsLedgerEntry>()

        val goalMap = goals.map { it.copy() }.associateBy { it.id }.toMutableMap()
        var changed = false

        var available = savingsAvailable(now, shifts, expenses, recurringExpenses, goalMap.values.toList(), currentLedger, settings, workplaces)

        val dueGoalIds = goalMap.values.filter { it.auto && it.autoNext.isNotEmpty() && it.autoNext <= today }
            .sortedWith(compareBy<SavingsGoal> { it.autoNext }.thenBy { it.id })
            .map { it.id }

        for (gid in dueGoalIds) {
            var g = goalMap[gid]!!
            var iterations = 0
            while (iterations < 1200 && g.autoNext <= today && cents(g.saved) < cents(g.target)) {
                iterations++
                val remaining = cents(g.target) - cents(g.saved)
                val amount = if (g.autoAmount > 0) {
                    minOf(cents(g.autoAmount), remaining)
                } else {
                    minOf(maxOf(0L, available), remaining)
                }
                if (amount <= 0 || available < amount) {
                    val overdue = if (g.autoAmount > 0) {
                        maxOf(0L, amount - maxOf(0L, available))
                    } else {
                        minOf(remaining, maxOf(0L, -available))
                    }
                    val overdueDouble = overdue / 100.0
                    if (g.autoOverdue != overdueDouble) {
                        g = g.copy(autoOverdue = overdueDouble)
                        goalMap[gid] = g
                        changed = true
                    }
                    break
                }
                val entryId = "${g.id}:${g.autoEpoch}:${g.autoNext}"
                if (currentLedger.none { it.id == entryId }) {
                    val entry = SavingsLedgerEntry(
                        id = entryId,
                        goalId = g.id,
                        date = today,
                        scheduled = g.autoNext,
                        amount = amount / 100.0
                    )
                    currentLedger.add(entry)
                    newLedgerEntries.add(entry)
                    val newSaved = (cents(g.saved) + amount) / 100.0
                    val nextDate = nextSavingsDate(g.autoNext, g.autoInterval, g.autoAnchor)
                    g = g.copy(
                        saved = newSaved,
                        autoOverdue = 0.0,
                        autoNext = nextDate
                    )
                    goalMap[gid] = g
                    available -= amount
                    changed = true
                } else {
                    val nextDate = nextSavingsDate(g.autoNext, g.autoInterval, g.autoAnchor)
                    g = g.copy(
                        autoOverdue = 0.0,
                        autoNext = nextDate
                    )
                    goalMap[gid] = g
                    changed = true
                }
            }
        }
        return ReconcileResult(goalMap.values.toList(), newLedgerEntries, changed)
    }

    fun isoCalendarWeek(date: LocalDate): Pair<Int, Int> {
        val week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
        return Pair(year, week)
    }
}
