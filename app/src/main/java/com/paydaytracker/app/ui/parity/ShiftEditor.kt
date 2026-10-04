package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paydaytracker.app.data.*
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters as F
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable fun ShiftEditor(vm: MainViewModel, shift: Shift?, initialDate: String, template: ShiftTemplate?, dismiss: () -> Unit) {
    val places by vm.workplaces.collectAsState()
    val settings by vm.settings.collectAsState()
    val templates by vm.shiftTemplates.collectAsState()
    val shifts by vm.shifts.collectAsState()
    val filter by vm.workplaceFilter.collectAsState()
    var date by rememberSaveable { mutableStateOf(shift?.date ?: initialDate) }
    var start by rememberSaveable { mutableStateOf(shift?.start ?: template?.start ?: "12:00") }
    var end by rememberSaveable { mutableStateOf(shift?.end ?: template?.end ?: "20:00") }
    var pause by rememberSaveable { mutableStateOf((shift?.breakMin ?: template?.breakMin ?: 30).toString()) }
    var note by rememberSaveable { mutableStateOf(shift?.note ?: template?.note ?: "") }
    var workplace by rememberSaveable { mutableStateOf(shift?.workplaceId ?: places.firstOrNull { it.id == filter }?.id ?: places.firstOrNull()?.id ?: "") }
    var manual by rememberSaveable { mutableStateOf(shift?.statusSource == "manual") }
    var status by rememberSaveable { mutableStateOf(shift?.status ?: "completed") }
    var cancelledBy by rememberSaveable { mutableStateOf(shift?.cancelledBy?.let { if (it == "self") "me" else it } ?: "unspecified") }
    var wage by rememberSaveable { mutableStateOf(shift?.wage?.toString() ?: "") }
    var error by remember { mutableStateOf(false) }
    var multi by rememberSaveable { mutableStateOf(false) }
    // A saveable list also avoids duplicate records when a date is tapped repeatedly.
    var selectedDates by rememberSaveable { mutableStateOf(listOf(date)) }
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    var calendarMonth by rememberSaveable { mutableStateOf(date.take(7)) }
    var templateName by rememberSaveable { mutableStateOf("") }
    var templateOptions by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val dates = if (multi && shift == null) selectedDates.toSet() else setOf(date)
    val parsed = runCatching {
        val d = LocalDate.parse(date)
        val a = LocalTime.parse(start); val b = LocalTime.parse(end)
        val minutes = Duration.between(a, b).toMinutes().let { if (it <= 0) it + 1440 else it }
        val p = pause.toInt(); require(p >= 0 && p < minutes)
        require(places.any { it.id == workplace })
        val w = wage.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDouble()
        require(w == null || w.isFinite() && w > 0)
        Triple(d, minutes.toInt() - p, w)
    }.getOrNull()
    fun automaticStatus(d: String) = if (LocalDateTime.parse(d + "T" + start).isAfter(LocalDateTime.now())) "planned" else "completed"
    val effective = if (manual) status else automaticStatus(date)
    val save = {
        if (parsed == null || dates.isEmpty()) error = true else {
            val records = dates.sorted().map { d ->
                val state = if (manual) effective else automaticStatus(d)
                (shift ?: Shift(UUID.randomUUID().toString(), d, start, end, parsed.second)).copy(
                    date = d, start = start, end = end, minutes = parsed.second, breakMin = pause.toInt(),
                    workplaceId = workplace, wage = parsed.third, status = state,
                    statusSource = if (manual) "manual" else "auto", cancelledBy = if (state == "cancelled") cancelledBy else null,
                    note = note, completedAutomatically = false)
            }
            vm.saveShifts(records)
            dismiss()
        }
    }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().fillMaxHeight(.94f).padding(horizontal = 12.dp, vertical = 8.dp).testTag("shift-editor"),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Eyebrow(L("Working time", "Arbeitszeit"))
                        Heading(if (shift == null) L("Add shift", "Schicht hinzufügen") else L("Edit shift", "Schicht bearbeiten"))
                    }
                    val close = L("Close shift editor", "Schichtfenster schließen")
                    OutlinedIconButton(dismiss, Modifier.size(44.dp).semantics { contentDescription = close }, shape = RoundedCornerShape(14.dp)) { Mark("close") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("shift-editor-content"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (templates.isNotEmpty()) Choice(L("Shift template", "Schichtvorlage"), "", listOf("" to L("No template", "Keine Vorlage")) + templates.map { it.id to it.name }) { id ->
                        templates.firstOrNull { it.id == id }?.let { start = it.start; end = it.end; pause = it.breakMin.toString(); note = it.note }
                    }
                    Choice(L("Workplace", "Arbeitsplatz"), workplace, places.map { it.id to it.name }) { workplace = it }
                    PickerField(L("Date", "Datum"), LocalDate.parse(date).format(DateTimeFormatter.ofPattern(if (LocalLanguage.current == "en") "dd/MM/yyyy" else "dd.MM.yyyy")), tag = "shift-date") {
                        calendarMonth = date.take(7); calendarOpen = !calendarOpen
                    }
                    if (shift == null) {
                        Row(Modifier.fillMaxWidth().toggleable(value = multi, role = Role.Checkbox) { enabled ->
                            multi = enabled; selectedDates = listOf(date); calendarMonth = date.take(7); calendarOpen = enabled
                        }.padding(vertical = 4.dp).testTag("multiple-shift-days"), verticalAlignment = Alignment.CenterVertically) {
                            Text(L("Select multiple specific days", "Mehrere einzelne Tage auswählen"), Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Checkbox(multi, null)
                        }
                    }
                    if (calendarOpen || multi) {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CalendarMonthHeader(calendarMonth, { calendarMonth = it }, centered = true)
                                CalendarGrid(calendarMonth, shifts.filter { it.workplaceId == workplace }, selectedDates = dates, picker = true) { chosen ->
                                    if (multi) selectedDates = if (chosen in selectedDates) selectedDates - chosen else selectedDates + chosen
                                    else { date = chosen; selectedDates = listOf(chosen); calendarOpen = false }
                                }
                                if (multi) Text("${dates.size} " + if (dates.size == 1) L("day selected", "Tag ausgewählt") else L("days selected", "Tage ausgewählt"), Modifier.align(Alignment.CenterHorizontally), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShiftTimeField(L("Start", "Beginn"), start, { start = it }, Modifier.weight(1f), "shift-start-time")
                        ShiftTimeField(L("End", "Ende"), end, { end = it }, Modifier.weight(1f), "shift-end-time")
                    }
                    Field(L("Unpaid break (minutes)", "Pause (Minuten)"), pause, { pause = it })
                    Choice(L("Status", "Status"), effective, listOf("planned", "completed", "cancelled").map { it to statusLabel(it) }) { status = it; manual = true }
                    Toggle(L("Automatic status", "Automatischer Status"), checked = !manual) { manual = !it }
                    if (effective == "cancelled") Choice(L("Cancelled by", "Abgesagt durch"), cancelledBy, listOf("unspecified" to L("Not specified", "Nicht angegeben"), "employer" to L("Employer", "Arbeitgeber"), "me" to L("Me", "Mich"))) { cancelledBy = it }
                    Field(L("Note (optional)", "Notiz (optional)"), note, { note = it })
                    Field(L("Hourly wage (optional)", "Stundenlohn (optional)"), wage, { wage = it })
                    if (parsed != null) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Purple, contentColor = WhiteInk) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Stat(L("Paid time", "Bezahlte Zeit"), F.formatDuration(if (effective == "cancelled") 0 else parsed.second))
                                Stat(L("Base pay", "Grundlohn"), F.formatMoney((if (effective == "cancelled") 0 else parsed.second) / 60.0 * (parsed.third ?: places.firstOrNull { it.id == workplace }?.wage ?: settings.wage), settings.currency))
                            }
                        }
                    }
                    TextButton({ templateOptions = !templateOptions }) { Text(L("Shift templates", "Schichtvorlagen")) }
                    if (templateOptions) {
                        Field(L("Template name", "Vorlagenname"), templateName, { templateName = it })
                        Action(L("Save as template", "Als Vorlage speichern"), enabled = parsed != null && templateName.isNotBlank()) {
                            vm.saveTemplate(ShiftTemplate(UUID.randomUUID().toString(), templateName, start, end, pause.toInt(), effective, note)); templateName = ""
                        }
                        templates.forEach { t -> TextButton({ vm.deleteTemplate(t.id) }) { Text(L("Delete template: ", "Vorlage löschen: ") + t.name) } }
                    }
                    if (shift != null) TextButton({ confirmDelete = true }) { Text(L("Delete shift", "Schicht löschen"), color = MaterialTheme.colorScheme.error) }
                    if (error) Text(L("Select a day and check the break and wage.", "Tag auswählen und Pause und Lohn prüfen."), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                }
                HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.outline)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Action(L("Cancel", "Abbrechen"), Modifier.weight(1f)) { dismiss() }
                    Action(L("Save shift", "Schicht speichern"), Modifier.weight(1.2f).testTag("save-shift"), primary = true, enabled = dates.isNotEmpty(), onClick = save)
                }
            }
        }
    }
    if (confirmDelete && shift != null) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(L("Delete this shift?", "Diese Schicht löschen?")) },
        confirmButton = { TextButton({ vm.deleteShift(shift.id); dismiss() }) { Text(L("Delete", "Löschen")) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(L("Cancel", "Abbrechen")) } })
}
