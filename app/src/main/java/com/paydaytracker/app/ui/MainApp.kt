package com.paydaytracker.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paydaytracker.app.MainActivity
import com.paydaytracker.app.data.RecurringExpense
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.ui.backup.BackupScreen
import com.paydaytracker.app.ui.dashboard.DashboardScreen
import com.paydaytracker.app.ui.expenses.ExpenseEditDialog
import com.paydaytracker.app.ui.expenses.ExpensesScreen
import com.paydaytracker.app.ui.navigation.BottomNavItems
import com.paydaytracker.app.ui.navigation.Screen
import com.paydaytracker.app.ui.payslips.PayslipsScreen
import com.paydaytracker.app.ui.planning.GoalEditDialog
import com.paydaytracker.app.ui.planning.PlanningScreen
import com.paydaytracker.app.ui.profile.ProfileScreen
import com.paydaytracker.app.ui.settings.SettingsScreen
import com.paydaytracker.app.ui.shifts.ShiftEditDialog
import com.paydaytracker.app.ui.shifts.ShiftsScreen
import com.paydaytracker.app.ui.theme.WageTrackTheme
import com.paydaytracker.app.ui.workplaces.WorkplacesScreen
import java.util.UUID

@Composable
fun MainApp(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settings by viewModel.settings.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val workplaces by viewModel.workplaces.collectAsState()

    var activeShiftForEdit by remember { mutableStateOf<Shift?>(null) }
    var showShiftDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (settings.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    WageTrackTheme(darkTheme = isDark) {
        Scaffold(
            bottomBar = {
                val isBottomNavDestination = BottomNavItems.any { it.route == currentRoute }
                if (isBottomNavDestination) {
                    NavigationBar {
                        BottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = {
                                    if (screen.icon != null) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                },
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onAddShiftClick = {
                            activeShiftForEdit = null
                            showShiftDialog = true
                        },
                        onEditShiftClick = { shiftId ->
                            activeShiftForEdit = shifts.firstOrNull { it.id == shiftId }
                            showShiftDialog = true
                        }
                    )
                }
                composable(Screen.Shifts.route) {
                    ShiftsScreen(
                        viewModel = viewModel,
                        onAddShiftClick = {
                            activeShiftForEdit = null
                            showShiftDialog = true
                        },
                        onEditShiftClick = { shiftId ->
                            activeShiftForEdit = shifts.firstOrNull { it.id == shiftId }
                            showShiftDialog = true
                        }
                    )
                }
                composable(Screen.Expenses.route) {
                    ExpensesScreen(
                        viewModel = viewModel,
                        onAddExpenseClick = { showExpenseDialog = true }
                    )
                }
                composable(Screen.Planning.route) {
                    PlanningScreen(
                        viewModel = viewModel,
                        onAddGoalClick = { showGoalDialog = true }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToWorkplaces = { navController.navigate(Screen.Workplaces.route) },
                        onNavigateToPayslips = { navController.navigate(Screen.Payslips.route) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onNavigateToSecurity = {
                            MainActivity.appLockInstance?.let { lock ->
                                lock.change(!lock.enabled)
                            }
                        },
                        onNavigateToBackup = { navController.navigate(Screen.Backup.route) }
                    )
                }
                composable(Screen.Workplaces.route) {
                    WorkplacesScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Payslips.route) {
                    PayslipsScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Backup.route) {
                    BackupScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        if (showShiftDialog) {
            ShiftEditDialog(
                shift = activeShiftForEdit,
                workplaces = workplaces,
                defaultWage = settings.wage,
                onDismiss = { showShiftDialog = false },
                onSave = { shift ->
                    viewModel.saveShift(shift)
                    showShiftDialog = false
                },
                onDelete = { shiftId ->
                    viewModel.deleteShift(shiftId)
                    showShiftDialog = false
                }
            )
        }

        if (showExpenseDialog) {
            ExpenseEditDialog(
                onDismiss = { showExpenseDialog = false },
                onSaveExpense = { expense, isRecurring ->
                    viewModel.saveExpense(expense)
                    if (isRecurring) {
                        viewModel.saveRecurringExpense(
                            RecurringExpense(
                                id = expense.recurringId ?: UUID.randomUUID().toString(),
                                startMonth = expense.date.substring(0, 7),
                                day = expense.date.substring(8).toIntOrNull() ?: 1,
                                amount = expense.amount,
                                category = expense.category,
                                note = expense.note
                            )
                        )
                    }
                    showExpenseDialog = false
                }
            )
        }

        if (showGoalDialog) {
            GoalEditDialog(
                onDismiss = { showGoalDialog = false },
                onSaveGoal = { goal ->
                    viewModel.saveSavingsGoal(goal)
                    showGoalDialog = false
                }
            )
        }
    }
}
