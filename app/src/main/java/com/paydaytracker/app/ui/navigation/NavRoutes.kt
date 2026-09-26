package com.paydaytracker.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Übersicht", Icons.Default.Dashboard)
    object Shifts : Screen("shifts", "Schichten", Icons.Default.CalendarMonth)
    object Expenses : Screen("expenses", "Ausgaben", Icons.Default.AccountBalanceWallet)
    object Planning : Screen("planning", "Planung", Icons.Default.PieChart)
    object Settings : Screen("settings", "Einstellungen", Icons.Default.Settings)

    // Sub-screens
    object Workplaces : Screen("workplaces", "Arbeitsplätze")
    object Payslips : Screen("payslips", "Lohnabrechnungen")
    object Profile : Screen("profile", "Profil")
    object Security : Screen("security", "Sicherheit & Sperre")
    object Backup : Screen("backup", "Sichern & Wiederherstellen")
}

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Shifts,
    Screen.Expenses,
    Screen.Planning,
    Screen.Settings
)
