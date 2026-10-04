package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.BorderStroke
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
            shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("⌄", fontSize = 17.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ShiftTimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, tag: String = "", placeholder: String = "") {
    var open by remember { mutableStateOf(false) }
    PickerField(label, value.ifBlank { placeholder }, modifier, tag) { open = true }
    if (open) {
        val initial = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.NOON)
        val time = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.widthIn(max = 380.dp).fillMaxWidth().padding(12.dp).testTag("time-dialog"), shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, Modifier.fillMaxWidth().padding(bottom = 12.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    // Dial-only input. Cancelling or pressing Back leaves the original value untouched.
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        TimePicker(time, Modifier.testTag("time-picker"), colors = TimePickerDefaults.colors(
                            clockDialColor = Raised, clockDialSelectedContentColor = Ink,
                            clockDialUnselectedContentColor = WhiteInk, selectorColor = Lime,
                            containerColor = MaterialTheme.colorScheme.surface,
                            timeSelectorSelectedContainerColor = Lime, timeSelectorSelectedContentColor = Ink))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Action(L("Cancel", "Abbrechen"), Modifier.weight(1f)) { open = false }
                        Action(L("Set time", "Zeit übernehmen"), Modifier.weight(1f).testTag("confirm-time"), primary = true) {
                            onChange(LocalTime.of(time.hour, time.minute).format(DateTimeFormatter.ofPattern("HH:mm")))
                            open = false
                        }
                    }
                }
            }
        }
    }
}
