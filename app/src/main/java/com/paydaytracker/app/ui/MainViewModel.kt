package com.paydaytracker.app.ui

import android.app.Application
import com.paydaytracker.app.data.DataMigration
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paydaytracker.app.NativeCoordinator
import com.paydaytracker.app.data.AppSettings
import com.paydaytracker.app.data.CustomCategory
import com.paydaytracker.app.data.Expense
import com.paydaytracker.app.data.MonthForecast
import com.paydaytracker.app.data.MonthSummary
import com.paydaytracker.app.data.PayrollCalculator
import com.paydaytracker.app.data.Payslip
import com.paydaytracker.app.data.RecurringExpense
import com.paydaytracker.app.data.SavingsGoal
import com.paydaytracker.app.data.SavingsLedgerEntry
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.data.ShiftTemplate
import com.paydaytracker.app.data.SpendingProjection
import com.paydaytracker.app.data.UserProfile
import com.paydaytracker.app.data.WageRepository
import com.paydaytracker.app.data.Workplace
import com.paydaytracker.app.data.db.WageTrackDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WageTrackDatabase.getInstance(application)
    val repository = WageRepository(db)

    val ready = MutableStateFlow(false)
    val startupError = MutableStateFlow<String?>(null)
    val document = repository.document.stateIn(viewModelScope, SharingStarted.Eagerly, "{}")
    fun initialize() { viewModelScope.launch {
        startupError.value = null
        try {
            DataMigration.checkAndMigrate(getApplication(), repository)
            val doc = repository.document()
            if (repository.workplaces.first().isNotEmpty() && !doc.has("onboardingCompleted")) repository.updateDocument { it.put("onboardingCompleted", true) }
            val language = doc.optString("language", getApplication<Application>().getSharedPreferences("device", 0).getString("language", "de"))
            getApplication<Application>().getSharedPreferences("device", 0).edit().putString("language", language).apply()
            ready.value = true
            repository.reconcileShiftStatuses()
            repository.materializeRecurringExpenses(selectedMonth.value)
            repository.reconcileSavings()
        } catch (e: Exception) { startupError.value = e.message ?: "Unable to load saved data" }
    } }
    fun updateDocument(edit: (JSONObject) -> Unit) { viewModelScope.launch { repository.updateDocument(edit); triggerBackup() } }
    fun saveTemplate(t: ShiftTemplate) { viewModelScope.launch { repository.saveShiftTemplate(t); triggerBackup() } }
    fun deleteTemplate(id: String) { viewModelScope.launch { repository.deleteShiftTemplate(id); triggerBackup() } }
    fun deleteRecurring(id: String) { viewModelScope.launch { repository.deleteRecurringExpense(id); triggerBackup() } }
    fun setup(s: AppSettings, name: String, job: String) { viewModelScope.launch {
        repository.saveSettings(s); repository.saveProfile(UserProfile(name = name))
        if (repository.workplaces.first().isEmpty()) repository.addWorkplace(job, s.wage)
        repository.updateDocument { it.put("onboardingCompleted", true) }; triggerBackup()
    } }
    fun resetData() { viewModelScope.launch { repository.clearAll(); repository.updateDocument { it.put("onboardingCompleted", false) }; triggerBackup() } }

    private val currentYearMonth = YearMonth.now()
    val selectedMonth = MutableStateFlow(currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")))
    val workplaceFilter = MutableStateFlow("all")

    val workplaces: StateFlow<List<Workplace>> = repository.workplaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shifts: StateFlow<List<Shift>> = repository.shifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.expenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringExpenses: StateFlow<List<RecurringExpense>> = repository.recurringExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payslips: StateFlow<List<Payslip>> = repository.payslips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoal>> = repository.savingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsLedger: StateFlow<List<SavingsLedgerEntry>> = repository.savingsLedger
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<CustomCategory>> = repository.customCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shiftTemplates: StateFlow<List<ShiftTemplate>> = repository.shiftTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val profile: StateFlow<UserProfile> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val currentMonthSummary: StateFlow<MonthSummary?> = combine(
        selectedMonth,
        shifts,
        settings,
        workplaces,
        workplaceFilter
    ) { month, shiftList, appSettings, wpList, filter ->
        PayrollCalculator.summary(
            monthKey = month,
            shifts = shiftList,
            settings = appSettings,
            workplaces = wpList,
            filterWorkplaceId = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMonthForecast: StateFlow<MonthForecast?> = combine(
        currentMonthSummary,
        settings,
        selectedMonth
    ) { summary, appSettings, month ->
        if (summary != null) {
            PayrollCalculator.forecast(
                s = summary,
                settings = appSettings,
                selected = month
            )
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentSpendingProjection: StateFlow<SpendingProjection?> = combine(
        selectedMonth,
        expenses
    ) { month, expList ->
        PayrollCalculator.spendingProjection(month, expList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savingsAvailableCents: StateFlow<Long> = combine(
        shifts,
        expenses,
        recurringExpenses,
        savingsGoals,
        savingsLedger
    ) { s, e, r, g, l ->
        PayrollCalculator.savingsAvailable(
            now = LocalDateTime.now(),
            shifts = s,
            expenses = e,
            recurringExpenses = r,
            savingsGoals = g,
            savingsLedger = l,
            settings = settings.value,
            workplaces = workplaces.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        viewModelScope.launch { while (true) { delay(60000); if (ready.value) { if (repository.reconcileShiftStatuses()) triggerBackup(); repository.reconcileSavings() } } }

        viewModelScope.launch {
            combine(document, ready) { raw, loaded -> if (loaded) {
                val doc = JSONObject(raw)
                NativeCoordinator.syncTimer(getApplication(), doc)
                doc.optJSONObject("loggingReminder")?.let { c ->
                    getApplication<Application>().getSharedPreferences("device", 0).edit().putBoolean("reminder", c.optBoolean("enabled")).putInt("reminderDays", c.optInt("days",62)).putInt("reminderHour",c.optInt("hour",20)).putInt("reminderMinute",c.optInt("minute",0)).apply()
                    com.paydaytracker.app.ReminderReceiver.schedule(getApplication())
                }
            } }.collect {}
        }
        // Reactive synchronization with native system services (Phase 4)
        viewModelScope.launch {
            combine(currentMonthSummary, settings) { summary, s ->
                if (ready.value) NativeCoordinator.syncWidget(getApplication(), summary, s)
            }.collect {}
        }

        viewModelScope.launch {
            combine(shifts, workplaces, document, ready) { shiftList, wpList, doc, loaded ->
                if (loaded) NativeCoordinator.syncShiftReminders(getApplication(), shiftList, wpList, JSONObject(doc))
            }.collect {}
        }

        viewModelScope.launch {
            combine(payslips, settings) { payslipList, s ->
                NativeCoordinator.syncPayslipReminder(getApplication(), payslipList, s.payslipReminder)
            }.collect {}
        }
    }

    private fun triggerBackup() {
        NativeCoordinator.syncAutoBackup(getApplication(), repository, viewModelScope)
    }

    fun prevMonth() {
        val ym = YearMonth.parse(selectedMonth.value).minusMonths(1)
        selectedMonth.value = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        viewModelScope.launch {
            repository.materializeRecurringExpenses(selectedMonth.value)
        }
    }

    fun nextMonth() {
        val ym = YearMonth.parse(selectedMonth.value).plusMonths(1)
        selectedMonth.value = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        viewModelScope.launch {
            repository.materializeRecurringExpenses(selectedMonth.value)
        }
    }

    fun setMonth(monthKey: String) {
        selectedMonth.value = monthKey
        viewModelScope.launch {
            repository.materializeRecurringExpenses(selectedMonth.value)
        }
    }

    fun setFilter(filter: String) {
        workplaceFilter.value = filter
    }

    fun saveTimedShift(shift: Shift) { viewModelScope.launch { repository.saveTimedShift(shift); triggerBackup() } }
    fun saveShift(shift: Shift) {
        viewModelScope.launch {
            repository.saveShift(shift)
            triggerBackup()
        }
    }

    fun deleteShift(id: String) {
        viewModelScope.launch {
            repository.deleteShift(id)
            triggerBackup()
        }
    }

    fun deleteShifts(ids: List<String>) {
        viewModelScope.launch {
            repository.deleteShifts(ids)
            triggerBackup()
        }
    }

    fun saveExpense(expense: Expense) {
        viewModelScope.launch {
            repository.saveExpense(expense)
            triggerBackup()
        }
    }

    fun saveRecurringExpense(recurring: RecurringExpense) {
        viewModelScope.launch {
            repository.saveRecurringExpense(recurring)
            triggerBackup()
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            triggerBackup()
        }
    }

    fun addWorkplace(name: String, wage: Double) {
        viewModelScope.launch {
            repository.addWorkplace(name, wage)
            triggerBackup()
        }
    }

    fun updateWorkplace(workplace: Workplace) {
        viewModelScope.launch {
            repository.updateWorkplace(workplace)
            triggerBackup()
        }
    }

    fun deleteWorkplace(id: String) {
        viewModelScope.launch {
            repository.deleteWorkplace(id)
            triggerBackup()
        }
    }

    fun saveSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.saveSavingsGoal(goal)
            repository.reconcileSavings()
            triggerBackup()
        }
    }

    fun deleteSavingsGoal(id: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
            triggerBackup()
        }
    }

    fun saveCustomCategory(category: CustomCategory) {
        viewModelScope.launch {
            repository.saveCustomCategory(category)
            triggerBackup()
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            triggerBackup()
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            triggerBackup()
        }
    }

    fun savePayslip(payslip: Payslip) {
        viewModelScope.launch {
            repository.savePayslip(payslip)
            triggerBackup()
        }
    }

    fun deletePayslip(id: String) {
        viewModelScope.launch {
            repository.deletePayslip(id)
            triggerBackup()
        }
    }
}
