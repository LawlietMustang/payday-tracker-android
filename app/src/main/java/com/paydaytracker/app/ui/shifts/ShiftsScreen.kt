package com.paydaytracker.app.ui.shifts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters
import com.paydaytracker.app.ui.dashboard.ShiftItemRow
import com.paydaytracker.app.ui.theme.StatusCancelled
import com.paydaytracker.app.ui.theme.StatusCompleted
import com.paydaytracker.app.ui.theme.StatusPlanned
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ShiftsScreen(
    viewModel: MainViewModel,
    onAddShiftClick: () -> Unit,
    onEditShiftClick: (String) -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val workplaces by viewModel.workplaces.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val monthShifts = shifts.filter { it.date.startsWith(selectedMonth) }
    val selectedShiftIds = remember { mutableStateListOf<String>() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddShiftClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Schicht hinzufügen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            // View Tabs (Kalender / Liste)
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Kalender") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Liste (${monthShifts.size})") }
                )
            }

            if (selectedShiftIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedShiftIds.size} ausgewählt",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            viewModel.deleteShifts(selectedShiftIds.toList())
                            selectedShiftIds.clear()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Ausgewählte löschen", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    CalendarView(
                        monthKey = selectedMonth,
                        shifts = monthShifts,
                        onDayClick = { date ->
                            // Filter or open shift
                            val dayShift = monthShifts.firstOrNull { it.date == date }
                            if (dayShift != null) onEditShiftClick(dayShift.id) else onAddShiftClick()
                        }
                    )
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (monthShifts.isEmpty()) {
                            item {
                                Text(
                                    text = "Keine Schichten in diesem Monat eingetragen.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(monthShifts) { shift ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedShiftIds.contains(shift.id),
                                        onCheckedChange = { checked ->
                                            if (checked) selectedShiftIds.add(shift.id) else selectedShiftIds.remove(shift.id)
                                        }
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
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
                }
            }
        }
    }
}

@Composable
fun CalendarView(
    monthKey: String,
    shifts: List<Shift>,
    onDayClick: (String) -> Unit
) {
    val parts = monthKey.split("-").map { it.toInt() }
    val yearMonth = YearMonth.of(parts[0], parts[1])
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value // 1=Mon .. 7=Sun

    val weekDays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    Column(modifier = Modifier.padding(16.dp)) {
        // Weekday labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekDays.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days Grid
        val totalCells = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7 * 7
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(totalCells) { index ->
                val dayNum = index - (firstDayOfWeek - 2)
                if (dayNum in 1..daysInMonth) {
                    val dateStr = String.format("%s-%02d", monthKey, dayNum)
                    val dayShifts = shifts.filter { it.date == dateStr }
                    val hasCompleted = dayShifts.any { it.status == "completed" }
                    val hasPlanned = dayShifts.any { it.status == "planned" }
                    val hasCancelled = dayShifts.any { it.status == "cancelled" }

                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onDayClick(dateStr) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                hasCompleted -> StatusCompleted.copy(alpha = 0.15f)
                                hasPlanned -> StatusPlanned.copy(alpha = 0.15f)
                                hasCancelled -> StatusCancelled.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (dayShifts.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                )
                                if (dayShifts.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (hasCompleted) DotIndicator(StatusCompleted)
                                        if (hasPlanned) DotIndicator(StatusPlanned)
                                        if (hasCancelled) DotIndicator(StatusCancelled)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
fun DotIndicator(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color)
    )
}
