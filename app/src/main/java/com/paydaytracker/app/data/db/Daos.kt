package com.paydaytracker.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkplaceDao {
    @Query("SELECT * FROM workplaces WHERE NOT archived ORDER BY name ASC")
    fun getAllFlow(): Flow<List<Workplace>>

    @Query("SELECT * FROM workplaces")
    suspend fun getAll(): List<Workplace>

    @Query("SELECT * FROM workplaces WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Workplace?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workplace: Workplace)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workplaces: List<Workplace>)

    @Update
    suspend fun update(workplace: Workplace)

    @Delete
    suspend fun delete(workplace: Workplace)

    @Query("DELETE FROM workplaces WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY date DESC, created DESC")
    fun getAllFlow(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, created DESC")
    fun getByMonthFlow(monthPrefix: String): Flow<List<Shift>>

    @Query("SELECT * FROM shifts ORDER BY date DESC, created DESC")
    suspend fun getAll(): List<Shift>

    @Query("SELECT * FROM shifts WHERE date LIKE :monthPrefix || '%'")
    suspend fun getByMonth(monthPrefix: String): List<Shift>

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shift: Shift)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<Shift>)

    @Update
    suspend fun update(shift: Shift)

    @Delete
    suspend fun delete(shift: Shift)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shifts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, created DESC")
    fun getAllFlow(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, created DESC")
    fun getByMonthFlow(monthPrefix: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, created DESC")
    suspend fun getAll(): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expenses WHERE recurringId = :recurringId AND date >= :fromDate")
    suspend fun deleteRecurringFromDate(recurringId: String, fromDate: String)
}

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY startMonth ASC, day ASC")
    fun getAllFlow(): Flow<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAll(): List<RecurringExpense>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringExpense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringExpense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recurring: List<RecurringExpense>)

    @Update
    suspend fun update(recurring: RecurringExpense)

    @Delete
    suspend fun delete(recurring: RecurringExpense)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PayslipDao {
    @Query("SELECT * FROM payslips ORDER BY month DESC, created DESC")
    fun getAllFlow(): Flow<List<Payslip>>

    @Query("SELECT * FROM payslips ORDER BY month DESC, created DESC")
    suspend fun getAll(): List<Payslip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payslip: Payslip)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payslips: List<Payslip>)

    @Delete
    suspend fun delete(payslip: Payslip)

    @Query("DELETE FROM payslips WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY autoNext ASC, id ASC")
    fun getAllFlow(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAll(): List<SavingsGoal>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoal>)

    @Update
    suspend fun update(goal: SavingsGoal)

    @Delete
    suspend fun delete(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SavingsLedgerDao {
    @Query("SELECT * FROM savings_ledger ORDER BY date ASC")
    fun getAllFlow(): Flow<List<SavingsLedgerEntry>>

    @Query("SELECT * FROM savings_ledger")
    suspend fun getAll(): List<SavingsLedgerEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SavingsLedgerEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<SavingsLedgerEntry>)
}

@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAllFlow(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories")
    suspend fun getAll(): List<CustomCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CustomCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CustomCategory>)

    @Delete
    suspend fun delete(category: CustomCategory)

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ShiftTemplateDao {
    @Query("SELECT * FROM shift_templates ORDER BY name ASC")
    fun getAllFlow(): Flow<List<ShiftTemplate>>

    @Query("SELECT * FROM shift_templates")
    suspend fun getAll(): List<ShiftTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: ShiftTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<ShiftTemplate>)

    @Delete
    suspend fun delete(template: ShiftTemplate)

    @Query("DELETE FROM shift_templates WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE month = :month LIMIT 1")
    fun getBudgetFlow(month: String): Flow<MonthlyBudget?>

    @Query("SELECT * FROM monthly_budgets WHERE month = :month LIMIT 1")
    suspend fun getBudget(month: String): MonthlyBudget?

    @Query("SELECT * FROM monthly_budgets")
    suspend fun getAll(): List<MonthlyBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: MonthlyBudget)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettings)

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}
