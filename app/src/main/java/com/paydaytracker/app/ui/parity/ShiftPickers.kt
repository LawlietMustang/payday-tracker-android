package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** A button, deliberately not an editable TextField: the saved value cannot be erased. */
@Composable fun PickerField(label: String, value: String, modifier: Modifier = Modifier, tag: String = "", onClick: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (label.isNotEmpty()) Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag).semantics { role = Role.Button },
            shape = RoundedCornerShape(14.dp), color = if (MaterialTheme.colorScheme.surface == Purple) Raised else androidx.compose.ui.graphics.Color(0xFFFFFAFF),
            contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("⌄", fontSize = 17.sp)
            }
        }
    }
}

/** 0 and 13–23 are explicit 24-hour input; 1–12 use the selected period. */
internal fun enteredHour(hour: String, pm: Boolean): Int? {
    val number = hour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    return if (number == 0 || number > 12) number else number % 12 + if (pm) 12 else 0
}

@Composable fun ShiftTimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, tag: String = "", placeholder: String = "") {
    var open by rememberSaveable { mutableStateOf(false) }
    PickerField(label, value.ifBlank { placeholder }, modifier, tag) { open = true }
    if (open) {
        val initial = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.NOON)
        var hour by rememberSaveable { mutableStateOf((initial.hour % 12).let { if (it == 0) "12" else it.toString().padStart(2, '0') }) }
        var minute by rememberSaveable { mutableStateOf(initial.minute.toString().padStart(2, '0')) }
        var pm by rememberSaveable { mutableStateOf(initial.hour >= 12) }
        var selectingHour by rememberSaveable { mutableStateOf(true) }
        val resolvedHour = enteredHour(hour, pm)
        val resolvedMinute = minute.toIntOrNull()?.takeIf { it in 0..59 }
        fun normalizeHour() {
            enteredHour(hour, pm)?.let { h ->
                pm = h >= 12
                hour = (h % 12).let { if (it == 0) "12" else it.toString().padStart(2, '0') }
            }
        }
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            val focus = LocalFocusManager.current
            Surface(Modifier.widthIn(max = 340.dp).fillMaxWidth().imePadding().padding(12.dp).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.testTag("time-dialog"), shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, Modifier.fillMaxWidth().padding(bottom = 12.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeNumber(hour, L("Hour", "Stunde"), "time-hour-input", selectingHour, Modifier.weight(1f),
                                onFocus = { selectingHour = true }, onBlur = { normalizeHour() }, onDone = { focus.clearFocus() }) { text ->
                                hour = text
                                text.toIntOrNull()?.takeIf { it in 0..23 }?.let { pm = it >= 12 }
                            }
                            Text(":", fontSize = 34.sp, modifier = Modifier.padding(bottom = 20.dp))
                            TimeNumber(minute, L("Minute", "Minute"), "time-minute-input", !selectingHour, Modifier.weight(1f),
                                onFocus = { selectingHour = false }, onBlur = {
                                    minute.toIntOrNull()?.takeIf { it in 0..59 }?.let { minute = it.toString().padStart(2, '0') }
                                }, onDone = { focus.clearFocus() }) { minute = it }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (isPm in listOf(false, true)) {
                                    Surface(onClick = {
                                        focus.clearFocus(); normalizeHour(); pm = isPm
                                    }, modifier = Modifier.width(50.dp).height(34.dp).testTag(if (isPm) "time-pm" else "time-am").semantics { selected = pm == isPm },
                                        shape = RoundedCornerShape(10.dp), color = if (pm == isPm) Lime else Raised, contentColor = if (pm == isPm) Ink else WhiteInk) {
                                        Box(contentAlignment = Alignment.Center) { Text(if (isPm) "PM" else "AM", fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                        if (resolvedHour == null || resolvedMinute == null) {
                            Text(L("Hours: 0–23 · Minutes: 0–59", "Stunden: 0–23 · Minuten: 0–59"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        AnalogClock(selectingHour, if (selectingHour) (resolvedHour ?: 0) % 12 else resolvedMinute ?: 0,
                            select = { number ->
                                focus.clearFocus()
                                if (selectingHour) hour = (if (number == 0) 12 else number).toString().padStart(2, '0')
                                else minute = number.toString().padStart(2, '0')
                            }, finish = { if (selectingHour) selectingHour = false })
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Action(L("Cancel", "Abbrechen"), Modifier.weight(1f)) { open = false }
                        Action(L("Set time", "Zeit übernehmen"), Modifier.weight(1f).testTag("confirm-time"), primary = true,
                            enabled = resolvedHour != null && resolvedMinute != null) {
                            onChange(LocalTime.of(resolvedHour!!, resolvedMinute!!).format(DateTimeFormatter.ofPattern("HH:mm")))
                            open = false
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun TimeNumber(value: String, label: String, tag: String, active: Boolean, modifier: Modifier,
    onFocus: () -> Unit, onBlur: () -> Unit, onDone: () -> Unit, change: (String) -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (field.text != value) field = TextFieldValue(value, TextRange(value.length)) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(field, { next ->
            if (next.text.length <= 2 && next.text.all { it in '0'..'9' }) { field = next; change(next.text) }
        }, modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = 1f }.testTag(tag).onFocusChanged {
            if (it.isFocused && !focused) { onFocus(); field = field.copy(selection = TextRange(0, field.text.length)) }
            if (!it.isFocused && focused) onBlur()
            focused = it.isFocused
        }.semantics { contentDescription = label }, singleLine = true,
            textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, color = if (active) Ink else WhiteInk),
            cursorBrush = SolidColor(if (active) Ink else WhiteInk), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { inner -> Box(Modifier.background(if (active) Lime else Raised, RoundedCornerShape(12.dp)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) { inner() } })
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A circular dial with a rigid hand: angle selects the value, never the finger's radius. */
@Composable private fun AnalogClock(hours: Boolean, selectedNumber: Int, select: (Int) -> Unit, finish: () -> Unit) {
    val selectCurrent by rememberUpdatedState(select)
    val finishCurrent by rememberUpdatedState(finish)
    BoxWithConstraints(Modifier.widthIn(max = 256.dp).fillMaxWidth().aspectRatio(1f).testTag("time-picker")
        .pointerInput(hours) {
            awaitEachGesture {
                // Own the gesture before the surrounding form can treat it as scrolling.
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                val center = Offset(size.width / 2f, size.height / 2f)
                if ((down.position - center).getDistance() > size.width / 2f) return@awaitEachGesture
                down.consume()
                var chosen = false
                fun choose(point: Offset) {
                    val delta = point - center
                    if (delta.getDistance() > size.width * .10f) {
                        val angle = (atan2(delta.y, delta.x) + PI / 2 + 2 * PI) % (2 * PI)
                        val count = if (hours) 12 else 60
                        selectCurrent((angle / (2 * PI) * count).roundToInt() % count)
                        chosen = true
                    }
                }
                choose(down.position)
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (event.changes.size > 1) break
                    choose(change.position)
                    change.consume()
                    if (!change.pressed) {
                        // Switch to minutes only after release, never halfway through an hour drag.
                        if (chosen) finishCurrent()
                        break
                    }
                }
            }
        }) {
        val diameter = maxWidth
        val radius = diameter / 2 - 26.dp
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Raised)
            val angle = selectedNumber * 2 * PI / (if (hours) 12 else 60) - PI / 2
            val end = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius.toPx()
            drawLine(Lime, center, end, strokeWidth = 2.dp.toPx())
            drawCircle(Lime, 4.dp.toPx(), center)
            drawCircle(Lime, 22.dp.toPx(), end)
        }
        repeat(12) { index ->
            val value = if (hours) index else index * 5
            val number = if (hours && index == 0) 12 else value
            val angle = index * PI / 6 - PI / 2
            Box(Modifier.offset(x = diameter / 2 + radius * cos(angle).toFloat() - 22.dp, y = diameter / 2 + radius * sin(angle).toFloat() - 22.dp)
                .size(44.dp).graphicsLayer { alpha = 1f }
                .semantics {
                    contentDescription = if (hours) "$number hours" else "$number minutes"
                    role = Role.Button
                    onClick { selectCurrent(value); finishCurrent(); true }
                }, contentAlignment = Alignment.Center) {
                Text(if (hours) number.toString() else number.toString().padStart(2, '0'), color = if (selectedNumber == value) Ink else WhiteInk, fontSize = 18.sp)
            }
        }
    }
}
