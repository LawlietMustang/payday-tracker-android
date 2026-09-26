package com.paydaytracker.app.data

import com.paydaytracker.app.data.db.WageTrackDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class WageRepository(private val db: WageTrackDatabase) {

    val workplaces: Flow<List<Workplace>> = db.workplaceDao().getAllFlow()
    val shifts: Flow<List<Shift>> = db.shiftDao().getAllFlow()
    val expenses: Flow<List<Expense>> = db.expenseDao().getAllFlow()
    val recurringExpenses: Flow<List<RecurringExpense>> = db.recurringExpenseDao().getAllFlow()
    val payslips: Flow<List<Payslip>> = db.payslipDao().getAllFlow()
    val savingsGoals: Flow<List<SavingsGoal>> = db.savingsGoalDao().getAllFlow()
    val savingsLedger: Flow<List<SavingsLedgerEntry>> = db.savingsLedgerDao().getAllFlow()
    val customCategories: Flow<List<CustomCategory>> = db.customCategoryDao().getAllFlow()
    val shiftTemplates: Flow<List<ShiftTemplate>> = db.shiftTemplateDao().getAllFlow()

    val settings: Flow<AppSettings> = db.settingsDao().getSettingsFlow().map { it ?: AppSettings() }
    val profile: Flow<UserProfile> = db.settingsDao().getProfileFlow().map { it ?: UserProfile() }

    fun shiftsForMonth(monthPrefix: String): Flow<List<Shift>> = db.shiftDao().getByMonthFlow(monthPrefix)
    fun expensesForMonth(monthPrefix: String): Flow<List<Expense>> = db.expenseDao().getByMonthFlow(monthPrefix)
    fun budgetForMonth(month: String): Flow<MonthlyBudget?> = db.monthlyBudgetDao().getBudgetFlow(month)

    suspend fun getSettings(): AppSettings = db.settingsDao().getSettings() ?: AppSettings()
    suspend fun saveSettings(settings: AppSettings) = db.settingsDao().insertOrUpdate(settings)

    suspend fun getProfile(): UserProfile = db.settingsDao().getProfile() ?: UserProfile()
    suspend fun saveProfile(profile: UserProfile) = db.settingsDao().insertOrUpdateProfile(profile)

    suspend fun addWorkplace(name: String, wage: Double): Workplace {
        val wp = Workplace(id = UUID.randomUUID().toString(), name = name, wage = wage)
        db.workplaceDao().insert(wp)
        return wp
    }

    suspend fun updateWorkplace(workplace: Workplace) = db.workplaceDao().update(workplace)

    suspend fun deleteWorkplace(id: String) {
        val all = db.workplaceDao().getAll()
        if (all.size <= 1) return
        val fallback = all.firstOrNull { it.id != id } ?: return
        // Move shifts to fallback workplace
        val shifts = db.shiftDao().getAll().filter { it.workplaceId == id }
        val updated = shifts.map { it.copy(workplaceId = fallback.id) }
        db.shiftDao().insertAll(updated)
        // Remove payslips for this workplace
        val payslips = db.payslipDao().getAll().filter { it.workplaceId == id }
        payslips.forEach { db.payslipDao().delete(it) }
        db.workplaceDao().deleteById(id)
    }

    suspend fun saveShift(shift: Shift) = db.shiftDao().insert(shift)
    suspend fun saveShifts(shifts: List<Shift>) = db.shiftDao().insertAll(shifts)
    suspend fun deleteShift(id: String) = db.shiftDao().deleteById(id)
    suspend fun deleteShifts(ids: List<String>) = db.shiftDao().deleteByIds(ids)

    suspend fun saveExpense(expense: Expense) = db.expenseDao().insert(expense)
    suspend fun deleteExpense(id: String) {
        val exp = db.expenseDao().getById(id) ?: return
        if (exp.recurringId != null) {
            db.recurringExpenseDao().deleteById(exp.recurringId)
            db.expenseDao().deleteRecurringFromDate(exp.recurringId, exp.date)
        } else {
            db.expenseDao().deleteById(id)
        }
    }

    suspend fun saveRecurringExpense(recurring: RecurringExpense) = db.recurringExpenseDao().insert(recurring)
    suspend fun deleteRecurringExpense(id: String) = db.recurringExpenseDao().deleteById(id)

    suspend fun savePayslip(payslip: Payslip) = db.payslipDao().insert(payslip)
    suspend fun deletePayslip(id: String) = db.payslipDao().deleteById(id)

    suspend fun saveSavingsGoal(goal: SavingsGoal) = db.savingsGoalDao().insert(goal)
    suspend fun deleteSavingsGoal(id: String) = db.savingsGoalDao().deleteById(id)

    suspend fun saveCustomCategory(category: CustomCategory) = db.customCategoryDao().insert(category)
    suspend fun deleteCustomCategory(id: String) = db.customCategoryDao().deleteById(id)

    suspend fun saveShiftTemplate(template: ShiftTemplate) = db.shiftTemplateDao().insert(template)
    suspend fun deleteShiftTemplate(id: String) = db.shiftTemplateDao().deleteById(id)

    suspend fun saveBudget(budget: MonthlyBudget) = db.monthlyBudgetDao().insertOrUpdate(budget)

    suspend fun reconcileShiftStatuses(now: LocalDateTime = LocalDateTime.now()): Boolean {
        val s = getSettings()
        if (!s.autoCompletePlanned) return false
        val allShifts = db.shiftDao().getAll()
        var changed = false
        val toUpdate = mutableListOf<Shift>()
        for (shift in allShifts) {
            if (shift.status == "planned") {
                val range = PayrollCalculator.shiftRange(shift)
                if (!range.end.isAfter(now)) {
                    toUpdate.add(shift.copy(status = "completed", completedAutomatically = true))
                    changed = true
                }
            }
        }
        if (changed) {
            db.shiftDao().insertAll(toUpdate)
        }
        return changed
    }

    suspend fun materializeRecurringExpenses(monthKey: String) {
        val recurring = db.recurringExpenseDao().getAll()
        val expenses = db.expenseDao().getAll()
        val parts = monthKey.split("-").map { it.toInt() }
        val lastDay = YearMonth.of(parts[0], parts[1]).lengthOfMonth()
        val toInsert = mutableListOf<Expense>()

        recurring.forEach { t ->
            if (t.startMonth <= monthKey) {
                val day = minOf(t.day, lastDay)
                val date = String.format("%s-%02d", monthKey, day)
                val alreadyMaterialized = expenses.any { it.recurringId == t.id && it.date.startsWith(monthKey) }
                if (!alreadyMaterialized) {
                    toInsert.add(
                        Expense(
                            id = UUID.randomUUID().toString(),
                            date = date,
                            amount = t.amount,
                            category = t.category,
                            note = t.note,
                            recurringId = t.id
                        )
                    )
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            db.expenseDao().insertAll(toInsert)
        }
    }

    suspend fun reconcileSavings(now: LocalDateTime = LocalDateTime.now()): Boolean {
        val shifts = db.shiftDao().getAll()
        val expenses = db.expenseDao().getAll()
        val recurring = db.recurringExpenseDao().getAll()
        val goals = db.savingsGoalDao().getAll()
        val ledger = db.savingsLedgerDao().getAll()
        val settings = getSettings()
        val workplaces = db.workplaceDao().getAll()

        val res = PayrollCalculator.reconcileSavings(now, shifts, expenses, recurring, goals, ledger, settings, workplaces)
        if (res.changed) {
            db.savingsGoalDao().insertAll(res.updatedGoals)
            if (res.newLedgerEntries.isNotEmpty()) {
                db.savingsLedgerDao().insertAll(res.newLedgerEntries)
            }
        }
        return res.changed
    }

    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("app", "PaydayTracker")
        root.put("version", 1)
        root.put("created", java.time.Instant.now().toString())

        val data = JSONObject()
        val s = getSettings()
        val p = getProfile()

        val sObj = JSONObject()
        sObj.put("wage", s.wage)
        sObj.put("target", s.target)
        sObj.put("savingsTarget", s.savingsTarget)
        sObj.put("theme", s.theme)
        sObj.put("currency", s.currency)
        sObj.put("taxMode", s.taxMode)
        sObj.put("deductionPercent", s.deductionPercent)
        sObj.put("taxclass", s.taxclass)
        sObj.put("state", s.state)
        sObj.put("health", s.health)
        sObj.put("church", s.church)
        sObj.put("childless", s.childless)
        val daysArray = JSONArray()
        s.days.forEach { daysArray.put(it) }
        sObj.put("days", daysArray)
        sObj.put("overtimeAfter", s.overtimeAfter)
        sObj.put("overtimeRate", s.overtimeRate)
        sObj.put("nightStart", s.nightStart)
        sObj.put("nightEnd", s.nightEnd)
        sObj.put("nightRate", s.nightRate)
        sObj.put("sundayRate", s.sundayRate)
        sObj.put("holidayRate", s.holidayRate)
        sObj.put("autoCompletePlanned", s.autoCompletePlanned)
        sObj.put("payslipReminder", s.payslipReminder)
        data.put("settings", sObj)

        val pObj = JSONObject()
        pObj.put("name", p.name)
        pObj.put("street", p.street)
        pObj.put("city", p.city)
        pObj.put("taxId", p.taxId)
        data.put("profile", pObj)

        val wpArray = JSONArray()
        db.workplaceDao().getAll().forEach { w ->
            val o = JSONObject()
            o.put("id", w.id)
            o.put("name", w.name)
            o.put("wage", w.wage)
            wpArray.put(o)
        }
        data.put("workplaces", wpArray)

        val shiftArray = JSONArray()
        db.shiftDao().getAll().forEach { sh ->
            val o = JSONObject()
            o.put("id", sh.id)
            o.put("date", sh.date)
            o.put("start", sh.start)
            o.put("end", sh.end)
            o.put("minutes", sh.minutes)
            o.put("breakMin", sh.breakMin)
            if (sh.wage != null) o.put("wage", sh.wage)
            o.put("workplaceId", sh.workplaceId)
            o.put("status", sh.status)
            o.put("statusSource", sh.statusSource)
            if (sh.cancelledBy != null) o.put("cancelledBy", sh.cancelledBy)
            if (sh.note != null) o.put("note", sh.note)
            o.put("created", sh.created)
            shiftArray.put(o)
        }
        data.put("shifts", shiftArray)

        val expArray = JSONArray()
        db.expenseDao().getAll().forEach { ex ->
            val o = JSONObject()
            o.put("id", ex.id)
            o.put("date", ex.date)
            o.put("amount", ex.amount)
            o.put("category", ex.category)
            if (ex.note != null) o.put("note", ex.note)
            if (ex.recurringId != null) o.put("recurringId", ex.recurringId)
            o.put("created", ex.created)
            expArray.put(o)
        }
        data.put("expenses", expArray)

        val recArray = JSONArray()
        db.recurringExpenseDao().getAll().forEach { rx ->
            val o = JSONObject()
            o.put("id", rx.id)
            o.put("startMonth", rx.startMonth)
            o.put("day", rx.day)
            o.put("amount", rx.amount)
            o.put("category", rx.category)
            if (rx.note != null) o.put("note", rx.note)
            o.put("created", rx.created)
            recArray.put(o)
        }
        data.put("recurringExpenses", recArray)

        val payArray = JSONArray()
        db.payslipDao().getAll().forEach { ps ->
            val o = JSONObject()
            o.put("id", ps.id)
            o.put("month", ps.month)
            o.put("workplaceId", ps.workplaceId)
            o.put("actualGross", ps.actualGross)
            o.put("actualNet", ps.actualNet)
            o.put("created", ps.created)
            payArray.put(o)
        }
        data.put("payslips", payArray)

        val goalArray = JSONArray()
        db.savingsGoalDao().getAll().forEach { g ->
            val o = JSONObject()
            o.put("id", g.id)
            o.put("name", g.name)
            o.put("target", g.target)
            o.put("saved", g.saved)
            o.put("due", g.due)
            o.put("auto", g.auto)
            o.put("autoInterval", g.autoInterval)
            o.put("autoAmount", g.autoAmount)
            o.put("autoAnchor", g.autoAnchor)
            o.put("autoNext", g.autoNext)
            o.put("autoEpoch", g.autoEpoch)
            o.put("autoOverdue", g.autoOverdue)
            goalArray.put(o)
        }
        data.put("savingsGoals", goalArray)

        val ledgerArray = JSONArray()
        db.savingsLedgerDao().getAll().forEach { le ->
            val o = JSONObject()
            o.put("id", le.id)
            o.put("goalId", le.goalId)
            o.put("date", le.date)
            o.put("scheduled", le.scheduled)
            o.put("amount", le.amount)
            ledgerArray.put(o)
        }
        data.put("savingsLedger", ledgerArray)

        val catArray = JSONArray()
        db.customCategoryDao().getAll().forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            catArray.put(o)
        }
        data.put("customCategories", catArray)

        root.put("data", data)
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: root

            val workplaces = mutableListOf<Workplace>()
            val wpArray = data.optJSONArray("workplaces")
            if (wpArray != null) {
                for (i in 0 until wpArray.length()) {
                    val o = wpArray.getJSONObject(i)
                    workplaces.add(Workplace(id = o.getString("id"), name = o.getString("name"), wage = o.getDouble("wage")))
                }
            }

            val shifts = mutableListOf<Shift>()
            val shArray = data.optJSONArray("shifts")
            if (shArray != null) {
                for (i in 0 until shArray.length()) {
                    val o = shArray.getJSONObject(i)
                    shifts.add(
                        Shift(
                            id = o.getString("id"),
                            date = o.getString("date"),
                            start = o.getString("start"),
                            end = o.getString("end"),
                            minutes = o.getInt("minutes"),
                            breakMin = o.optInt("breakMin", 0),
                            wage = if (o.has("wage")) o.getDouble("wage") else null,
                            workplaceId = o.optString("workplaceId", "default"),
                            status = o.optString("status", "completed"),
                            statusSource = o.optString("statusSource", "manual"),
                            cancelledBy = if (o.has("cancelledBy")) o.getString("cancelledBy") else null,
                            note = if (o.has("note")) o.getString("note") else null,
                            created = o.optLong("created", System.currentTimeMillis())
                        )
                    )
                }
            }

            val expenses = mutableListOf<Expense>()
            val exArray = data.optJSONArray("expenses")
            if (exArray != null) {
                for (i in 0 until exArray.length()) {
                    val o = exArray.getJSONObject(i)
                    expenses.add(
                        Expense(
                            id = o.getString("id"),
                            date = o.getString("date"),
                            amount = o.getDouble("amount"),
                            category = o.getString("category"),
                            note = if (o.has("note")) o.getString("note") else null,
                            recurringId = if (o.has("recurringId")) o.getString("recurringId") else null,
                            created = o.optLong("created", System.currentTimeMillis())
                        )
                    )
                }
            }

            val recurring = mutableListOf<RecurringExpense>()
            val rxArray = data.optJSONArray("recurringExpenses")
            if (rxArray != null) {
                for (i in 0 until rxArray.length()) {
                    val o = rxArray.getJSONObject(i)
                    recurring.add(
                        RecurringExpense(
                            id = o.getString("id"),
                            startMonth = o.getString("startMonth"),
                            day = o.getInt("day"),
                            amount = o.getDouble("amount"),
                            category = o.getString("category"),
                            note = if (o.has("note")) o.getString("note") else null,
                            created = o.optLong("created", System.currentTimeMillis())
                        )
                    )
                }
            }

            val payslips = mutableListOf<Payslip>()
            val psArray = data.optJSONArray("payslips")
            if (psArray != null) {
                for (i in 0 until psArray.length()) {
                    val o = psArray.getJSONObject(i)
                    payslips.add(
                        Payslip(
                            id = o.getString("id"),
                            month = o.getString("month"),
                            workplaceId = o.getString("workplaceId"),
                            actualGross = o.getDouble("actualGross"),
                            actualNet = o.getDouble("actualNet"),
                            created = o.optLong("created", System.currentTimeMillis())
                        )
                    )
                }
            }

            val goals = mutableListOf<SavingsGoal>()
            val sgArray = data.optJSONArray("savingsGoals")
            if (sgArray != null) {
                for (i in 0 until sgArray.length()) {
                    val o = sgArray.getJSONObject(i)
                    goals.add(
                        SavingsGoal(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            target = o.getDouble("target"),
                            saved = o.optDouble("saved", 0.0),
                            due = o.optString("due", ""),
                            auto = o.optBoolean("auto", false),
                            autoInterval = o.optString("autoInterval", "monthly"),
                            autoAmount = o.optDouble("autoAmount", 0.0),
                            autoAnchor = o.optInt("autoAnchor", 1),
                            autoNext = o.optString("autoNext", ""),
                            autoEpoch = o.optString("autoEpoch", ""),
                            autoOverdue = o.optDouble("autoOverdue", 0.0)
                        )
                    )
                }
            }

            val ledger = mutableListOf<SavingsLedgerEntry>()
            val slArray = data.optJSONArray("savingsLedger")
            if (slArray != null) {
                for (i in 0 until slArray.length()) {
                    val o = slArray.getJSONObject(i)
                    ledger.add(
                        SavingsLedgerEntry(
                            id = o.getString("id"),
                            goalId = o.getString("goalId"),
                            date = o.getString("date"),
                            scheduled = o.getString("scheduled"),
                            amount = o.getDouble("amount")
                        )
                    )
                }
            }

            val categories = mutableListOf<CustomCategory>()
            val ccArray = data.optJSONArray("customCategories")
            if (ccArray != null) {
                for (i in 0 until ccArray.length()) {
                    val o = ccArray.getJSONObject(i)
                    categories.add(CustomCategory(id = o.getString("id"), name = o.getString("name")))
                }
            }

            if (data.has("settings")) {
                val sObj = data.getJSONObject("settings")
                val days = mutableListOf<Int>()
                val dArr = sObj.optJSONArray("days")
                if (dArr != null) {
                    for (i in 0 until dArr.length()) days.add(dArr.getInt(i))
                } else days.addAll(listOf(1, 2, 3, 4, 5))

                val s = AppSettings(
                    wage = sObj.optDouble("wage", 15.0),
                    target = sObj.optDouble("target", 160.0),
                    savingsTarget = sObj.optDouble("savingsTarget", 500.0),
                    theme = sObj.optString("theme", "system"),
                    currency = sObj.optString("currency", "EUR"),
                    taxMode = sObj.optString("taxMode", "germany"),
                    deductionPercent = sObj.optDouble("deductionPercent", 0.0),
                    taxclass = sObj.optString("taxclass", "I"),
                    state = sObj.optString("state", "Hessen"),
                    health = sObj.optDouble("health", 2.9),
                    church = sObj.optBoolean("church", false),
                    childless = sObj.optBoolean("childless", true),
                    days = days,
                    overtimeAfter = sObj.optDouble("overtimeAfter", 160.0),
                    overtimeRate = sObj.optDouble("overtimeRate", 25.0),
                    nightStart = sObj.optString("nightStart", "22:00"),
                    nightEnd = sObj.optString("nightEnd", "06:00"),
                    nightRate = sObj.optDouble("nightRate", 25.0),
                    sundayRate = sObj.optDouble("sundayRate", 50.0),
                    holidayRate = sObj.optDouble("holidayRate", 100.0),
                    autoCompletePlanned = sObj.optBoolean("autoCompletePlanned", true),
                    payslipReminder = sObj.optBoolean("payslipReminder", false)
                )
                saveSettings(s)
            }

            if (data.has("profile")) {
                val pObj = data.getJSONObject("profile")
                val p = UserProfile(
                    name = pObj.optString("name", ""),
                    street = pObj.optString("street", ""),
                    city = pObj.optString("city", ""),
                    taxId = pObj.optString("taxId", "")
                )
                saveProfile(p)
            }

            if (workplaces.isNotEmpty()) db.workplaceDao().insertAll(workplaces)
            if (shifts.isNotEmpty()) db.shiftDao().insertAll(shifts)
            if (expenses.isNotEmpty()) db.expenseDao().insertAll(expenses)
            if (recurring.isNotEmpty()) db.recurringExpenseDao().insertAll(recurring)
            if (payslips.isNotEmpty()) db.payslipDao().insertAll(payslips)
            if (goals.isNotEmpty()) db.savingsGoalDao().insertAll(goals)
            if (ledger.isNotEmpty()) db.savingsLedgerDao().insertAll(ledger)
            if (categories.isNotEmpty()) db.customCategoryDao().insertAll(categories)

            true
        } catch (_: Exception) {
            false
        }
    }
}
