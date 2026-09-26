package com.paydaytracker.app.ui.dashboard

import com.paydaytracker.app.data.PayrollCalculator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters
import com.paydaytracker.app.ui.theme.AccentLime
import com.paydaytracker.app.ui.theme.AccentPink
import com.paydaytracker.app.ui.theme.AccentPurple
import com.paydaytracker.app.ui.theme.StatusCancelled
import com.paydaytracker.app.ui.theme.StatusCompleted
import com.paydaytracker.app.ui.theme.StatusPlanned

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onAddShiftClick: () -> Unit,
    onEditShiftClick: (String) -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val workplaceFilter by viewModel.workplaceFilter.collectAsState()
    val workplaces by viewModel.workplaces.collectAsState()
    val summary by viewModel.currentMonthSummary.collectAsState()
    val forecast by viewModel.currentMonthForecast.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var workplaceDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddShiftClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Schicht eintragen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Switcher Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.prevMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Vorheriger Monat")
                    }
                    Text(
                        text = Formatters.formatMonthTitle(selectedMonth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Nächster Monat")
                    }
                }
            }

            // Workplace Filter
            item {
                ExposedDropdownMenuBox(
                    expanded = workplaceDropdownExpanded,
                    onExpandedChange = { workplaceDropdownExpanded = !workplaceDropdownExpanded }
                ) {
                    val currentName = if (workplaceFilter == "all") "Alle Arbeitsplätze" else (workplaces.firstOrNull { it.id == workplaceFilter }?.name ?: "Arbeitsplatz")
                    OutlinedTextField(
                        value = currentName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workplaceDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = workplaceDropdownExpanded,
                        onDismissRequest = { workplaceDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Alle Arbeitsplätze") },
                            onClick = {
                                viewModel.setFilter("all")
                                workplaceDropdownExpanded = false
                            }
                        )
                        workplaces.forEach { wp ->
                            DropdownMenuItem(
                                text = { Text(wp.name) },
                                onClick = {
                                    viewModel.setFilter(wp.id)
                                    workplaceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Primary Gross / Net Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "GESCHÄTZTES NETTO",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatMoney(summary?.est?.net ?: 0.0, settings.currency),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Brutto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatMoney(summary?.gross ?: 0.0, settings.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Arbeitszeit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatDuration(summary?.workedMinutes ?: 0),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Progress towards target hours
                        val targetMinutes = (settings.target * 60).toInt()
                        val workedMinutes = summary?.workedMinutes ?: 0
                        val progress = if (targetMinutes > 0) minOf(1.0f, workedMinutes.toFloat() / targetMinutes) else 0.0f
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentLime,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}% erreicht",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Ziel: ${settings.target.toInt()} h",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Bonuses Breakdown Card
            if (summary != null && summary!!.bonus.total > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Zuschläge gesamt",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+ ${Formatters.formatMoney(summary!!.bonus.total, settings.currency)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (summary!!.bonus.night > 0) {
                                BonusRow(label = "Nachtarbeit", amount = summary!!.bonus.night, currency = settings.currency)
                            }
                            if (summary!!.bonus.sunday > 0) {
                                BonusRow(label = "Sonntagsarbeit", amount = summary!!.bonus.sunday, currency = settings.currency)
                            }
                            if (summary!!.bonus.holiday > 0) {
                                BonusRow(label = "Feiertagsarbeit", amount = summary!!.bonus.holiday, currency = settings.currency)
                            }
                            if (summary!!.bonus.overtime > 0) {
                                BonusRow(label = "Überstunden", amount = summary!!.bonus.overtime, currency = settings.currency)
                            }
                        }
                    }
                }
            }

            // Forecast Card
            if (forecast != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monatsprognose",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Erwartetes Brutto: ${Formatters.formatMoney(forecast!!.gross, settings.currency)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Erwartete Zeit: ${Formatters.formatDuration(forecast!!.minutes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = forecast!!.confidence.uppercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Recent Shifts Section Header
            item {
                Text(
                    text = "Aktuelle Schichten",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Shifts list
            val shiftList = summary?.list ?: emptyList()
            if (shiftList.isEmpty()) {
                item {
                    Text(
                        text = "Keine Schichten in diesem Monat eingetragen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(shiftList.take(5)) { shift ->
                    ShiftItemRow(
                        shift = shift,
                        workplaceName = workplaces.firstOrNull { it.id == shift.workplaceId }?.name ?: "Hauptjob",
                        currency = settings.currency,
                        onClick = { onEditShiftClick(shift.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BonusRow(label: String, amount: Double, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = Formatters.formatMoney(amount, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ShiftItemRow(
    shift: Shift,
    workplaceName: String,
    currency: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shift.date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = shift.status)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$workplaceName · ${shift.start} - ${shift.end} (${shift.breakMin}m Pause)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatDuration(shift.minutes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (shift.wage != null) {
                    Text(
                        text = Formatters.formatMoney(PayrollCalculator.grossEntry(shift, shift.wage), currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "completed" -> StatusCompleted to "Erledigt"
        "planned" -> StatusPlanned to "Geplant"
        "cancelled" -> StatusCancelled to "Abgesagt"
        else -> MaterialTheme.colorScheme.primary to status
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
