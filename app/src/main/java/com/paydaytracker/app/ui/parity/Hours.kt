package com.paydaytracker.app.ui.parity

import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.paydaytracker.app.data.*
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters as F
import java.time.*
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable fun Hours(vm:MainViewModel,add:(String,ShiftTemplate?)->Unit,edit:(String)->Unit,manage:()->Unit,newWorkplace:()->Unit) {
    val month by vm.selectedMonth.collectAsState();val all by vm.shifts.collectAsState();val workplaces by vm.workplaces.collectAsState();val filter by vm.workplaceFilter.collectAsState();val templates by vm.shiftTemplates.collectAsState()
    val document by vm.document.collectAsState()
    val calendarShifts=all.filter{filter=="all"||it.workplaceId==filter}
    val timer=org.json.JSONObject(document).optJSONObject("activeTimer")
    val activeDate=timer?.takeIf{filter=="all"||it.optString("workplaceId")==filter}?.let{Instant.ofEpochMilli(it.optLong("startedAt")).atZone(ZoneId.systemDefault()).toLocalDate().toString()}
    val shifts=all.filter{it.date.startsWith(month)&&(filter=="all"||it.workplaceId==filter)}.sortedByDescending{it.date+it.start}
    var bulkEdit by remember{mutableStateOf(false)}
    var day by remember{mutableStateOf<String?>(null)};var selected by remember(month){mutableStateOf(setOf<String>())};var delete by remember{mutableStateOf(false)}
    Page {
        WorkplacePicker(vm,manage)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            PurpleAction(L("+ New workplace","+ Neuer Arbeitsplatz"),Modifier.weight(1f),onClick=newWorkplace)
            PurpleAction(L("Manage workplaces","Arbeitsplätze verwalten"),Modifier.weight(1f),onClick=manage)
        }
        WageCard(Modifier.testTag("shift-calendar"),color=Purple,padding=14.dp){CompositionLocalProvider(LocalContentColor provides WhiteInk){
            CalendarMonthHeader(month,vm::setMonth)
            CalendarGrid(month,calendarShifts,activeDate){day=it}
            CalendarLegend()
            HorizontalDivider(color=Lavender.copy(alpha=.3f));Eyebrow(L("Quick shift templates","Schnelle Schichtvorlagen"))
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){templates.forEach{t->PurpleAction(t.name){add(month+"-01",t)}};PurpleAction(L("+ Shift","+ Schicht")){add(if(LocalDate.now().toString().startsWith(month))LocalDate.now().toString()else month+"-01",null)}}
        }}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading(L("Your shifts","Deine Schichten"));TextButton({selected=if(selected.size==shifts.size)emptySet()else shifts.map{it.id}.toSet()}){Text(L("Select all","Alle auswählen"),color=Lime)}}
        val cancelled=shifts.filter{it.status=="cancelled"}
        if(cancelled.isNotEmpty())WageCard{Text(L("Cancelled: ","Abgesagt: ")+"${cancelled.size} / ${shifts.size}",fontWeight=FontWeight.SemiBold);Text(L("Employer: ","Arbeitgeber: ")+cancelled.count{it.cancelledBy=="employer"}+L(" · By me: "," · Von mir: ")+cancelled.count{it.cancelledBy in listOf("me","self")},fontSize=12.sp)}
        if(selected.isNotEmpty())WageCard{Text("${selected.size} "+L("selected","ausgewählt"));Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){Action(L("Edit selected","Auswahl bearbeiten")){bulkEdit=true};Action(L("Delete","Löschen")){delete=true};listOf("planned","completed","cancelled").forEach{status->Action(statusLabel(status)){shifts.filter{it.id in selected}.forEach{vm.saveShift(it.copy(status=status,statusSource="manual",cancelledBy=if(status=="cancelled")"unspecified"else null))};selected=emptySet()}};Action(L("Done","Fertig")){selected=emptySet()}}}
        if(shifts.isEmpty())WageCard{Text(L("No shifts this month.","Noch keine Schichten in diesem Monat."));Action(L("+ Add shift","+ Schicht hinzufügen"),primary=true){add(month+"-01",null)}}
        shifts.forEach{shift->WageCard{Row(verticalAlignment=Alignment.CenterVertically){if(selected.isNotEmpty())Checkbox(shift.id in selected,{selected=if(shift.id in selected)selected-shift.id else selected+shift.id})
            Box(Modifier.weight(1f).testTag("shift-${shift.id}")){ShiftRow(shift,workplaces.firstOrNull{it.id==shift.workplaceId}?.name ?: "",{if(selected.isEmpty())edit(shift.id)else selected=if(shift.id in selected)selected-shift.id else selected+shift.id},longClick={selected=selected+shift.id})}
        }}}
    }
    if(bulkEdit)BulkShiftEditor(vm,shifts.filter{it.id in selected}){bulkEdit=false;selected=emptySet()}
    if(day!=null)AlertDialog(onDismissRequest={day=null},title={Text(day!!)},text={Column{calendarShifts.filter{it.date==day}.forEach{shift->LinkRow(shift.start+"–"+shift.end,statusLabel(shift.status)){day=null;edit(shift.id)}};Action(L("+ Add shift","+ Schicht hinzufügen"),primary=true){val d=day!!;day=null;add(d,null)}}},confirmButton={TextButton({day=null}){Text(L("Close","Schließen"))}})
    if(delete)AlertDialog(onDismissRequest={delete=false},title={Text(L("Delete selected shifts?","Ausgewählte Schichten löschen?"))},confirmButton={TextButton({vm.deleteShifts(selected.toList());selected=emptySet();delete=false}){Text(L("Delete","Löschen"))}},dismissButton={TextButton({delete=false}){Text(L("Cancel","Abbrechen"))}})
}
@Composable fun statusLabel(status:String)=when(status){"planned"->L("Planned","Geplant");"cancelled"->L("Cancelled","Abgesagt");else->L("Completed","Erledigt")}
