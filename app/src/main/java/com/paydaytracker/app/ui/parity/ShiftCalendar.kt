package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.paydaytracker.app.data.PayrollCalculator
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.ui.common.Formatters as F
import java.time.LocalDate
import java.time.YearMonth

private val Planned = Color(0xFF4C8CFF)
private val Done = Color(0xFF35D07F)
private val Active = Color(0xFFFF9F1C)
private val Overlap = Color(0xFFFF4757)
private val Cancelled = Color(0xFFAC8398)

@Composable fun CalendarMonthHeader(month: String, change: (String) -> Unit, centered: Boolean = false) {
    val previous = L("Previous month", "Vorheriger Monat")
    val next = L("Next month", "Nächster Monat")
    @Composable fun arrow(label: String, glyph: String, amount: Long) {
        OutlinedIconButton({ change(YearMonth.parse(month).plusMonths(amount).toString()) },
            Modifier.size(44.dp).semantics { contentDescription = label },
            shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, LocalContentColor.current.copy(alpha = .25f)),
            colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = LocalContentColor.current)) {
            Text(glyph, fontSize = 30.sp)
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (centered) arrow(previous, "‹", -1)
        Column(Modifier.weight(1f), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            if (!centered) { Eyebrow(L("Your shifts", "Deine Schichten")); Spacer(Modifier.height(8.dp)) }
            Text(F.formatMonthTitle(month), fontSize = if (centered) 19.sp else 21.sp, fontWeight = FontWeight.Bold, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
        }
        if (!centered) arrow(previous, "‹", -1)
        arrow(next, "›", 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun CalendarLegend() {
    FlowRow(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Planned to L("Planned", "Geplant"), Done to L("Done", "Erledigt"), Active to L("Active", "Aktiv"), Overlap to L("Overlap", "Überschneidung"), Cancelled to L("Cancelled", "Abgesagt")).forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(7.dp).background(color, CircleShape))
                Text(label, fontSize = 11.sp, color = Lavender)
            }
        }
    }
}

@Composable fun CalendarGrid(
    month: String, shifts: List<Shift>, activeDate: String? = null,
    selectedDates: Set<String> = emptySet(), picker: Boolean = false, select: (String) -> Unit
) {
    val conflicts = remember(shifts) {
        val intervals = shifts.filter { it.status != "cancelled" }.map { it to PayrollCalculator.shiftRange(it) }.sortedBy { it.second.start }
        buildSet<String> {
            for (i in intervals.indices) {
                var j = i + 1
                while (j < intervals.size && intervals[j].second.start < intervals[i].second.end) {
                    add(intervals[i].first.id); add(intervals[j].first.id); j++
                }
            }
        }
    }
    val byDate = remember(shifts) { shifts.groupBy { it.date } }
    val ym = YearMonth.parse(month)
    val first = ym.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val rows = if (picker) 6 else (ym.lengthOfMonth() + first.dayOfWeek.value - 2) / 7 + 1
    val weekdays = if (LocalLanguage.current == "en") listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") else listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            weekdays.forEach { Text(it, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = LocalContentColor.current.copy(alpha = .7f)) }
        }
        repeat(rows) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { index ->
                    val date = start.plusDays((week * 7 + index).toLong())
                    val key = date.toString()
                    val entries = byDate[key].orEmpty()
                    val state = when {
                        key == activeDate -> "active"
                        entries.any { it.id in conflicts } -> "overlap"
                        entries.isNotEmpty() && entries.all { it.status == "cancelled" } -> "cancelled"
                        entries.any { it.status == "planned" } -> "planned"
                        entries.isNotEmpty() -> "completed"
                        else -> "empty"
                    }
                    val border = when (state) { "planned" -> Planned; "completed" -> Done; "active" -> Active; "overlap" -> Overlap; "cancelled" -> Cancelled; else -> Color(0xFF59437C) }
                    val background = when (state) { "planned" -> Color(0xFF263E79); "completed" -> Color(0xFF164939); "active" -> Active; "overlap" -> Color(0xFF582338); "cancelled" -> Color(0xFF3C304B); else -> Raised }
                    val foreground = when (state) { "active" -> Ink; "cancelled" -> Color(0xFFDCC3D1); else -> WhiteInk }
                    val chosen = key in selectedDates
                    val today = date == LocalDate.now()
                    val stateText = when (state) { "active" -> L("Active", "Aktiv"); "overlap" -> L("Overlap", "Überschneidung"); "empty" -> L("No shifts", "Keine Schichten"); else -> statusLabel(state) }
                    Surface(onClick = { select(key) }, modifier = Modifier.weight(1f).aspectRatio(1f)
                        .graphicsLayer { alpha = if (YearMonth.from(date) != ym && !chosen) .35f else 1f }
                        .testTag("${if (picker) "picker" else "calendar"}-day-$key")
                        .semantics { contentDescription = "$key, $stateText"; selected = chosen; stateDescription = stateText },
                        color = background, contentColor = foreground, shape = RoundedCornerShape(10.dp),
                        border = if (state == "cancelled" && !chosen && !today) null else BorderStroke(if (chosen) 3.dp else if (today) 2.dp else 1.dp, if (chosen || today) Lime else border)) {
                        Box(Modifier.fillMaxSize().then(if (state == "cancelled" && !chosen && !today) Modifier.drawBehind {
                            drawRoundRect(border, cornerRadius = CornerRadius(10.dp.toPx()), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))))
                        } else Modifier)) {
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (!picker && entries.isNotEmpty()) Text("•".repeat(entries.size.coerceAtMost(3)), fontSize = 8.sp, lineHeight = 8.sp)
                            }
                            if (chosen) Text("✓", Modifier.align(Alignment.TopEnd).background(Lime, CircleShape).padding(horizontal = 2.dp), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
