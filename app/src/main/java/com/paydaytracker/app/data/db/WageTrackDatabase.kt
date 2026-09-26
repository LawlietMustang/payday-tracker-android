package com.paydaytracker.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.paydaytracker.app.data.AppSettings
import com.paydaytracker.app.data.CustomCategory
import com.paydaytracker.app.data.Expense
import com.paydaytracker.app.data.MonthlyBudget
import com.paydaytracker.app.data.Payslip
import com.paydaytracker.app.data.RecurringExpense
import com.paydaytracker.app.data.SavingsGoal
import com.paydaytracker.app.data.SavingsLedgerEntry
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.data.ShiftTemplate
import com.paydaytracker.app.data.UserProfile
import com.paydaytracker.app.data.Workplace

@Database(
    entities = [
        Workplace::class,
        Shift::class,
        Expense::class,
        RecurringExpense::class,
        Payslip::class,
        SavingsGoal::class,
        SavingsLedgerEntry::class,
        CustomCategory::class,
        ShiftTemplate::class,
        MonthlyBudget::class,
        AppSettings::class,
        UserProfile::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WageTrackDatabase : RoomDatabase() {

    abstract fun workplaceDao(): WorkplaceDao
    abstract fun shiftDao(): ShiftDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun payslipDao(): PayslipDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun savingsLedgerDao(): SavingsLedgerDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun shiftTemplateDao(): ShiftTemplateDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: WageTrackDatabase? = null

        fun getInstance(context: Context): WageTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WageTrackDatabase::class.java,
                    "wagetrack.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
