package com.paydaytracker.app.ui.shifts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.data.Workplace
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftEditDialog(
    shift: Shift?,
    workplaces: List<Workplace>,
    defaultWage: Double,
    onDismiss: () -> Unit,
    onSave: (Shift) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var date by remember { mutableStateOf(shift?.date ?: LocalDate.now().toString()) }
    var start by remember { mutableStateOf(shift?.start ?: "08:00") }
    var end by remember { mutableStateOf(shift?.end ?: "16:00") }
    var breakMin by remember { mutableStateOf(shift?.breakMin?.toString() ?: "30") }
    var workplaceId by remember { mutableStateOf(shift?.workplaceId ?: workplaces.firstOrNull()?.id ?: "default") }
    var status by remember { mutableStateOf(shift?.status ?: "completed") }
    var cancelledBy by remember { mutableStateOf(shift?.cancelledBy ?: "employer") }
    var note by remember { mutableStateOf(shift?.note ?: "") }
    var wageOverride by remember { mutableStateOf(shift?.wage?.toString() ?: "") }

    var wpDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var cancelDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (shift != null) "Schicht bearbeiten" else "Neue Schicht") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Datum (JJJJ-MM-TT)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Workplace dropdown
                ExposedDropdownMenuBox(
                    expanded = wpDropdownExpanded,
                    onExpandedChange = { wpDropdownExpanded = !wpDropdownExpanded }
                ) {
                    val currentWpName = workplaces.firstOrNull { it.id == workplaceId }?.name ?: "Arbeitsplatz"
                    OutlinedTextField(
                        value = currentWpName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Arbeitsplatz") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wpDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = wpDropdownExpanded,
                        onDismissRequest = { wpDropdownExpanded = false }
                    ) {
                        workplaces.forEach { wp ->
                            DropdownMenuItem(
                                text = { Text(wp.name) },
                                onClick = {
                                    workplaceId = wp.id
                                    wpDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Beginn (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("Ende (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = breakMin,
                    onValueChange = { breakMin = it },
                    label = { Text("Pause (Minuten)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status dropdown
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                ) {
                    val statusLabel = when (status) {
                        "completed" -> "Erledigt"
                        "planned" -> "Geplant"
                        "cancelled" -> "Abgesagt"
                        else -> status
                    }
                    OutlinedTextField(
                        value = statusLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Erledigt") },
                            onClick = { status = "completed"; statusDropdownExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Geplant") },
                            onClick = { status = "planned"; statusDropdownExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Abgesagt") },
                            onClick = { status = "cancelled"; statusDropdownExpanded = false }
                        )
                    }
                }

                if (status == "cancelled") {
                    ExposedDropdownMenuBox(
                        expanded = cancelDropdownExpanded,
                        onExpandedChange = { cancelDropdownExpanded = !cancelDropdownExpanded }
                    ) {
                        val cancelLabel = if (cancelledBy == "employer") "Vom Arbeitgeber abgesagt" else "Von mir abgesagt"
                        OutlinedTextField(
                            value = cancelLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Abgesagt von") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cancelDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = cancelDropdownExpanded,
                            onDismissRequest = { cancelDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Vom Arbeitgeber abgesagt") },
                                onClick = { cancelledBy = "employer"; cancelDropdownExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Von mir abgesagt") },
                                onClick = { cancelledBy = "me"; cancelDropdownExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notiz (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = wageOverride,
                    onValueChange = { wageOverride = it },
                    label = { Text("Stundenlohn abweichend (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bMin = breakMin.toIntOrNull() ?: 0
                    val sTime = try { LocalTime.parse(start.trim()) } catch (_: Exception) { LocalTime.of(8, 0) }
                    val eTime = try { LocalTime.parse(end.trim()) } catch (_: Exception) { LocalTime.of(16, 0) }
                    var durationMin = Duration.between(sTime, eTime).toMinutes().toInt()
                    if (durationMin <= 0) durationMin += 1440 // overnight
                    val workingMinutes = maxOf(0, durationMin - bMin)

                    val wOverride = wageOverride.replace(",", ".").toDoubleOrNull()

                    val newShift = Shift(
                        id = shift?.id ?: UUID.randomUUID().toString(),
                        date = date.trim(),
                        start = start.trim(),
                        end = end.trim(),
                        minutes = workingMinutes,
                        breakMin = bMin,
                        wage = wOverride,
                        workplaceId = workplaceId,
                        status = status,
                        cancelledBy = if (status == "cancelled") cancelledBy else null,
                        note = note.trim().ifEmpty { null }
                    )
                    onSave(newShift)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            Row {
                if (shift != null && onDelete != null) {
                    TextButton(onClick = { onDelete(shift.id) }) {
                        Text("Löschen", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}
