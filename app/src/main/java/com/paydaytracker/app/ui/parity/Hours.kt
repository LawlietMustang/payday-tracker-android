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
@Composable fun Hours(vm:MainViewModel,add:(String,ShiftTemplate?)->Unit,edit:(String)->Unit,manage:()->Unit) {
    val month by vm.selectedMonth.collectAsState();val all by vm.shifts.collectAsState();val workplaces by vm.workplaces.collectAsState();val filter by vm.workplaceFilter.collectAsState();val templates by vm.shiftTemplates.collectAsState()
    val shifts=all.filter{it.date.startsWith(month)&&(filter=="all"||it.workplaceId==filter)}.sortedByDescending{it.date+it.start}
    var day by remember{mutableStateOf<String?>(null)};var selected by remember(month){mutableStateOf(setOf<String>())};var delete by remember{mutableStateOf(false)}
    Page {
        WorkplacePicker(vm,manage)
        WageCard(color=Purple){CompositionLocalProvider(LocalContentColor provides WhiteInk){Eyebrow(L("Your shifts","Deine Schichten"));Heading(F.formatMonthTitle(month));CalendarGrid(month,shifts){day=it}
            Text(L("● Planned   ● Completed   ● Cancelled","● Geplant   ● Erledigt   ● Abgesagt"),fontSize=10.sp,color=Lavender)
            HorizontalDivider(color=Lavender.copy(alpha=.3f));Eyebrow(L("Quick shift templates","Schnelle Schichtvorlagen"))
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){templates.forEach{t->Action(t.name){add(month+"-01",t)}};Action(L("+ Shift","+ Schicht"),primary=true){add(if(LocalDate.now().toString().startsWith(month))LocalDate.now().toString()else month+"-01",null)}}
        }}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading(L("Your shifts","Deine Schichten"));TextButton({selected=if(selected.size==shifts.size)emptySet()else shifts.map{it.id}.toSet()}){Text(L("Select all","Alle auswählen"),color=Lime)}}
        if(selected.isNotEmpty())WageCard{Text("${selected.size} "+L("selected","ausgewählt"));Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){Action(L("Delete","Löschen")){delete=true};listOf("planned","completed","cancelled").forEach{status->Action(statusLabel(status)){shifts.filter{it.id in selected}.forEach{vm.saveShift(it.copy(status=status,statusSource="manual",cancelledBy=if(status=="cancelled")"employer"else null))};selected=emptySet()}};Action(L("Done","Fertig")){selected=emptySet()}}}
        if(shifts.isEmpty())WageCard{Text(L("No shifts this month.","Noch keine Schichten in diesem Monat."));Action(L("+ Add shift","+ Schicht hinzufügen"),primary=true){add(month+"-01",null)}}
        shifts.forEach{shift->WageCard{Row(verticalAlignment=Alignment.CenterVertically){if(selected.isNotEmpty())Checkbox(shift.id in selected,{selected=if(shift.id in selected)selected-shift.id else selected+shift.id})
            Box(Modifier.weight(1f).testTag("shift-${shift.id}")){ShiftRow(shift,workplaces.firstOrNull{it.id==shift.workplaceId}?.name ?: "",{if(selected.isEmpty())edit(shift.id)else selected=if(shift.id in selected)selected-shift.id else selected+shift.id},longClick={selected=selected+shift.id})}
        }}}
    }
    if(day!=null)AlertDialog(onDismissRequest={day=null},title={Text(day!!)},text={Column{shifts.filter{it.date==day}.forEach{shift->LinkRow(shift.start+"–"+shift.end,statusLabel(shift.status)){day=null;edit(shift.id)}};Action(L("+ Add shift","+ Schicht hinzufügen"),primary=true){val d=day!!;day=null;add(d,null)}}},confirmButton={TextButton({day=null}){Text(L("Close","Schließen"))}})
    if(delete)AlertDialog(onDismissRequest={delete=false},title={Text(L("Delete selected shifts?","Ausgewählte Schichten löschen?"))},confirmButton={TextButton({vm.deleteShifts(selected.toList());selected=emptySet();delete=false}){Text(L("Delete","Löschen"))}},dismissButton={TextButton({delete=false}){Text(L("Cancel","Abbrechen"))}})
}
@Composable fun statusLabel(status:String)=when(status){"planned"->L("Planned","Geplant");"cancelled"->L("Cancelled","Abgesagt");else->L("Completed","Erledigt")}
@Composable fun CalendarGrid(month:String,shifts:List<Shift>,select:(String)->Unit) {
    val ym=YearMonth.parse(month);val start=ym.atDay(1).minusDays((ym.atDay(1).dayOfWeek.value-1).toLong());val rows=(ym.lengthOfMonth()+ym.atDay(1).dayOfWeek.value-2)/7+1
    Row(Modifier.fillMaxWidth()){(if(LocalLanguage.current=="en")listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")else listOf("Mo","Di","Mi","Do","Fr","Sa","So")).forEach{Text(it,Modifier.weight(1f),fontSize=10.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
    repeat(rows){week->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){repeat(7){index->val date=start.plusDays((week*7+index).toLong());val entries=shifts.filter{it.date==date.toString()};val planned=entries.any{it.status=="planned"};val cancelled=entries.isNotEmpty()&&entries.all{it.status=="cancelled"};val color=if(cancelled)Pink else if(planned)Color(0xFF4C8CFF)else Color(0xFF35D07F)
        Surface(onClick={select(date.toString())},modifier=Modifier.weight(1f).aspectRatio(.92f),color=if(entries.isEmpty())Raised else color.copy(alpha=.2f),shape=RoundedCornerShape(10.dp),border=if(date==LocalDate.now())BorderStroke(1.dp,Lime)else null){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(date.dayOfMonth.toString(),fontSize=14.sp,color=if(date.month==ym.month)WhiteInk else Lavender.copy(alpha=.4f));Text(if(entries.isEmpty())" "else "•".repeat(entries.size.coerceAtMost(3)),fontSize=9.sp,color=color)}}
    }}}
}
@Composable fun ShiftEditor(vm:MainViewModel,shift:Shift?,initialDate:String,template:ShiftTemplate?,dismiss:()->Unit) {
    val places by vm.workplaces.collectAsState();val settings by vm.settings.collectAsState();val templates by vm.shiftTemplates.collectAsState();val context=LocalContext.current
    var date by remember{mutableStateOf(shift?.date ?: initialDate)};var start by remember{mutableStateOf(shift?.start ?: template?.start ?: "12:00")};var end by remember{mutableStateOf(shift?.end ?: template?.end ?: "20:00")};var pause by remember{mutableStateOf((shift?.breakMin ?: template?.breakMin ?: 30).toString())};var note by remember{mutableStateOf(shift?.note ?: template?.note ?: "")};var workplace by remember{mutableStateOf(shift?.workplaceId ?: places.firstOrNull()?.id ?: "")}
    var manual by remember{mutableStateOf(shift?.statusSource=="manual")};var status by remember{mutableStateOf(shift?.status ?: "completed")};var cancelledBy by remember{mutableStateOf(shift?.cancelledBy ?: "employer")};var wage by remember{mutableStateOf(shift?.wage?.toString() ?: "")};var error by remember{mutableStateOf(false)};var moreDates by remember{mutableStateOf(setOf<String>())};var templateName by remember{mutableStateOf("")};var confirmDelete by remember{mutableStateOf(false)}
    val parsed=runCatching{val d=LocalDate.parse(date);val a=LocalTime.parse(start);val b=LocalTime.parse(end);val mins=Duration.between(a,b).toMinutes().let{if(it<=0)it+1440 else it};val p=pause.toInt();require(p>=0&&p<mins);require(workplace.isNotEmpty());val w=wage.takeIf{it.isNotBlank()}?.replace(',','.')?.toDouble();require(w==null||w.isFinite()&&w>0);Triple(d,mins.toInt()-p,w)}.getOrNull()
    val effective=if(manual)status else if(runCatching{LocalDateTime.parse(date+"T"+start).isAfter(LocalDateTime.now())}.getOrDefault(false))"planned"else "completed"
    AlertDialog(onDismissRequest=dismiss,title={Text(if(shift==null)L("Add shift","Schicht hinzufügen")else L("Edit shift","Schicht bearbeiten"))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        if(templates.isNotEmpty())Choice(L("Shift template","Schichtvorlage"),"",listOf("" to L("No template","Keine Vorlage"))+templates.map{it.id to it.name}){id->templates.firstOrNull{it.id==id}?.let{start=it.start;end=it.end;pause=it.breakMin.toString();note=it.note}}
        Choice(L("Workplace","Arbeitsplatz"),workplace,places.map{it.id to it.name}){workplace=it}
        Field(L("Date (YYYY-MM-DD)","Datum (JJJJ-MM-TT)"),date,{date=it})
        if(shift==null) {Action(L("+ More dates","+ Weitere Tage")){val d=runCatching{LocalDate.parse(date)}.getOrDefault(LocalDate.now());DatePickerDialog(context,{_,y,m,day->val chosen=LocalDate.of(y,m+1,day).toString();moreDates=if(chosen in moreDates)moreDates-chosen else moreDates+chosen},d.year,d.monthValue-1,d.dayOfMonth).show()};if(moreDates.isNotEmpty())TextButton({moreDates=emptySet()}){Text(moreDates.joinToString()+L(" · Clear"," · Leeren"),fontSize=11.sp)}}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Field(L("Start (HH:mm)","Beginn (HH:mm)"),start,{start=it},Modifier.weight(1f));Field(L("End (HH:mm)","Ende (HH:mm)"),end,{end=it},Modifier.weight(1f))}
        Field(L("Unpaid break (minutes)","Pause (Minuten)"),pause,{pause=it})
        Choice(L("Status","Status"),effective,listOf("planned","completed","cancelled").map{it to statusLabel(it)}){status=it;manual=true}
        Toggle(L("Automatic status","Automatischer Status"),checked=!manual){manual=!it}
        if(effective=="cancelled")Choice(L("Cancelled by","Abgesagt durch"),cancelledBy,listOf("employer" to L("Employer","Arbeitgeber"),"self" to L("Me","Mich"))){cancelledBy=it}
        Field(L("Note (optional)","Notiz (optional)"),note,{note=it});Field(L("Hourly wage (optional)","Stundenlohn (optional)"),wage,{wage=it})
        if(parsed!=null){Stat(L("Paid time","Bezahlte Zeit"),F.formatDuration(parsed.second));Stat(L("Base pay","Grundlohn"),F.formatMoney(parsed.second/60.0*(parsed.third ?: places.firstOrNull{it.id==workplace}?.wage ?: settings.wage),settings.currency))}
        Field(L("Template name","Vorlagenname"),templateName,{templateName=it});Action(L("Save as template","Als Vorlage speichern"),enabled=parsed!=null&&templateName.isNotBlank()){vm.saveTemplate(ShiftTemplate(UUID.randomUUID().toString(),templateName,start,end,pause.toInt(),effective,note));templateName=""}
        templates.forEach{t->TextButton({vm.deleteTemplate(t.id)}){Text(L("Delete template: ","Vorlage löschen: ")+t.name)}}
        if(error)Text(L("Check the date, times, break and wage.","Datum, Uhrzeiten, Pause und Lohn prüfen."),color=MaterialTheme.colorScheme.error)
    }},confirmButton={TextButton({if(parsed==null)error=true else {for(d in moreDates+date){val autoStatus=if(manual)effective else if(LocalDateTime.parse(d+"T"+start)>LocalDateTime.now())"planned"else "completed";vm.saveShift((shift ?: Shift(UUID.randomUUID().toString(),d,start,end,parsed.second)).copy(date=d,start=start,end=end,minutes=parsed.second,breakMin=pause.toInt(),workplaceId=workplace,wage=parsed.third,status=autoStatus,statusSource=if(manual)"manual"else "auto",cancelledBy=if(autoStatus=="cancelled")cancelledBy else null,note=note,completedAutomatically=false))};dismiss()}}){Text(L("Save shift","Schicht speichern"))}},dismissButton={Row{if(shift!=null)TextButton({confirmDelete=true}){Text(L("Delete","Löschen"))};TextButton(dismiss){Text(L("Cancel","Abbrechen"))}}})
    if(confirmDelete&&shift!=null)AlertDialog(onDismissRequest={confirmDelete=false},title={Text(L("Delete this shift?","Diese Schicht löschen?"))},confirmButton={TextButton({vm.deleteShift(shift.id);dismiss()}){Text(L("Delete","Löschen"))}},dismissButton={TextButton({confirmDelete=false}){Text(L("Cancel","Abbrechen"))}})
}
