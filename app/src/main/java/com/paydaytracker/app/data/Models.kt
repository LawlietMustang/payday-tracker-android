package com.paydaytracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workplaces")
data class Workplace(
    @PrimaryKey val id: String,
    val name: String,
    val wage: Double,
    val archived: Boolean = false
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val start: String, // HH:mm
    val end: String, // HH:mm
    val minutes: Int,
    val breakMin: Int = 0,
    val wage: Double? = null,
    val workplaceId: String = "default",
    val status: String = "completed", // "completed", "planned", "cancelled"
    val statusSource: String = "manual",
    val cancelledBy: String? = null,
    val note: String? = null,
    val created: Long = System.currentTimeMillis(),
    val plannedDate: String? = null,
    val plannedStart: String? = null,
    val plannedEnd: String? = null,
    val completedAutomatically: Boolean = false
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val amount: Double,
    val category: String, // rent, utilities, health, groceries, transport, other
    val note: String? = null,
    val recurringId: String? = null,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey val id: String,
    val startMonth: String, // YYYY-MM
    val day: Int,
    val amount: Double,
    val category: String,
    val note: String? = null,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "payslips")
data class Payslip(
    @PrimaryKey val id: String,
    val month: String, // YYYY-MM
    val workplaceId: String,
    val actualGross: Double,
    val actualNet: Double,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey val id: String,
    val name: String,
    val target: Double,
    val saved: Double = 0.0,
    val due: String = "",
    val defaultName: Boolean = false,
    val auto: Boolean = false,
    val autoInterval: String = "monthly", // weekly, monthly, yearly
    val autoAmount: Double = 0.0,
    val autoAnchor: Int = 1,
    val autoNext: String = "",
    val autoEpoch: String = "",
    val autoOverdue: Double = 0.0
)

@Entity(tableName = "savings_ledger")
data class SavingsLedgerEntry(
    @PrimaryKey val id: String,
    val goalId: String,
    val date: String,
    val scheduled: String,
    val amount: Double
)

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val street: String = "",
    val city: String = "",
    val taxId: String = ""
)

@Entity(tableName = "shift_templates")
data class ShiftTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val start: String,
    val end: String,
    val breakMin: Int = 0,
    val status: String = "completed",
    val note: String = ""
)

data class ActiveTimer(
    val shiftId: String? = null,
    val start: Long = 0L,
    val isBreak: Boolean = false,
    val breakStart: Long = 0L,
    val totalBreakMinutes: Int = 0
)

@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey val month: String, // YYYY-MM
    val total: Double = 0.0,
    val categoryLimits: Map<String, Double> = emptyMap()
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val wage: Double = 15.0,
    val target: Double = 160.0,
    val savingsTarget: Double = 500.0,
    val theme: String = "system",
    val currency: String = "EUR",
    val taxMode: String = "germany", // "germany" or "manual"
    val deductionPercent: Double = 0.0,
    val taxclass: String = "I", // I, II, III, IV, V, VI
    val state: String = "Hessen",
    val health: Double = 2.9, // additional contribution in %
    val church: Boolean = false,
    val childless: Boolean = true,
    val days: List<Int> = listOf(1, 2, 3, 4, 5), // 0=Sun, 1=Mon, ..., 6=Sat
    val overtimeAfter: Double = 160.0,
    val overtimeRate: Double = 25.0,
    val nightStart: String = "22:00",
    val nightEnd: String = "06:00",
    val nightRate: Double = 25.0,
    val sundayRate: Double = 50.0,
    val holidayRate: Double = 100.0,
    val autoCompletePlanned: Boolean = true,
    val payslipReminder: Boolean = false
)
