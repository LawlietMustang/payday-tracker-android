package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paydaytracker.app.data.Shift
import com.paydaytracker.app.ui.MainViewModel
import java.time.Duration
import java.time.LocalTime

@Composable fun BulkShiftEditor(vm: MainViewModel, shifts: List<Shift>, dismiss: () -> Unit) {
    val places by vm.workplaces.collectAsState()
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var pause by remember { mutableStateOf("") }
    var wage by remember { mutableStateOf("") }
    var workplace by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var cancelledBy by remember { mutableStateOf("unspecified") }
    var note by remember { mutableStateOf("") }
    var replaceNote by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val unchanged = L("Keep unchanged", "Unverändert lassen")
    AlertDialog(onDismissRequest = dismiss, title = { Text(L("Edit selected shifts", "Ausgewählte Schichten bearbeiten")) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(L("Blank fields keep each shift's existing value.", "Leere Felder behalten den bisherigen Wert jeder Schicht."))
            Choice(L("Workplace", "Arbeitsplatz"), workplace, listOf("" to unchanged) + places.map { it.id to it.name }) { workplace = it }
            ShiftTimeField(L("Start", "Beginn"), start, { start = it }, tag="bulk-start-time", placeholder=unchanged)
            if(start.isNotEmpty()) TextButton({start=""}) { Text(L("Keep start unchanged", "Beginn unverändert lassen")) }
            ShiftTimeField(L("End", "Ende"), end, { end = it }, tag="bulk-end-time", placeholder=unchanged)
            if(end.isNotEmpty()) TextButton({end=""}) { Text(L("Keep end unchanged", "Ende unverändert lassen")) }
            Field(L("Unpaid break (minutes)", "Pause (Minuten)"), pause, { pause = it })
            Field(L("Hourly wage", "Stundenlohn"), wage, { wage = it })
            Choice(L("Status", "Status"), status, listOf("" to unchanged) + listOf("planned", "completed", "cancelled").map { it to statusLabel(it) }) { status = it }
            if (status == "cancelled") Choice(L("Cancelled by", "Abgesagt durch"), cancelledBy, listOf("unspecified" to L("Not specified", "Nicht angegeben"), "employer" to L("Employer", "Arbeitgeber"), "me" to L("Me", "Mich"))) { cancelledBy = it }
            Toggle(L("Replace notes", "Notizen ersetzen"), checked = replaceNote) { replaceNote = it }
            if (replaceNote) Field(L("Note", "Notiz"), note, { note = it })
            if (error) Text(L("Check the times, break and wage. No shifts were changed.", "Zeiten, Pause und Lohn prüfen. Keine Schicht wurde geändert."), color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        TextButton({
            val updated = runCatching {
                shifts.map { old ->
                    val a = start.ifBlank { old.start }
                    val b = end.ifBlank { old.end }
                    val p = pause.takeIf { it.isNotBlank() }?.toInt() ?: old.breakMin
                    val gross = Duration.between(LocalTime.parse(a), LocalTime.parse(b)).toMinutes().let { if (it <= 0) it + 1440 else it }
                    require(p >= 0 && p < gross)
                    val w = wage.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDouble() ?: old.wage
                    require(w == null || w.isFinite() && w > 0)
                    val state = status.ifBlank { old.status }
                    old.copy(start = a, end = b, breakMin = p, minutes = (gross - p).toInt(), wage = w,
                        workplaceId = workplace.ifBlank { old.workplaceId }, status = state,
                        statusSource = if (status.isBlank()) old.statusSource else "manual",
                        cancelledBy = if (status == "cancelled") cancelledBy else if (state == "cancelled") old.cancelledBy else null,
                        note = if (replaceNote) note else old.note, completedAutomatically = if (status.isBlank()) old.completedAutomatically else false)
                }
            }.getOrNull()
            if (updated == null) error = true else { vm.saveShifts(updated); dismiss() }
        }) { Text(L("Save changes", "Änderungen speichern")) }
    }, dismissButton = { TextButton(dismiss) { Text(L("Cancel", "Abbrechen")) } })
}
