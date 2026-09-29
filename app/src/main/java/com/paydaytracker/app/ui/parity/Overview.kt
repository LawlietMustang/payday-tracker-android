package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.paydaytracker.app.data.*
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters as F
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.*
import java.time.temporal.WeekFields
import java.util.UUID

fun timerTotals(t: JSONObject, now: Long): Pair<Long,Long> = (t.optLong("workedMs") + if(t.optString("state")=="working") (now-t.optLong("segmentStartedAt",now)).coerceAtLeast(0) else 0L) to (t.optLong("breakMs") + if(t.optString("state")=="break") (now-t.optLong("breakStartedAt",now)).coerceAtLeast(0) else 0L)
fun clock(ms: Long): String { val s=ms/1000;return "%02d:%02d:%02d".format(s/3600,s/60%60,s%60) }

@Composable fun Overview(vm: MainViewModel, navigate: (String)->Unit, add: ()->Unit, edit: (String)->Unit) {
    val payslips by vm.payslips.collectAsState()
    val s by vm.currentMonthSummary.collectAsState(); val forecast by vm.currentMonthForecast.collectAsState(); val settings by vm.settings.collectAsState()
    val shifts by vm.shifts.collectAsState(); val workplaces by vm.workplaces.collectAsState(); val filter by vm.workplaceFilter.collectAsState(); val month by vm.selectedMonth.collectAsState(); val expenses by vm.expenses.collectAsState()
    val summary=s ?: return; val money: (Double)->String={F.formatMoney(it,settings.currency)}
    val all = PayrollCalculator.summary(month,shifts,settings,workplaces,"all")
    val next=shifts.filter{it.status=="planned" && (filter=="all"||it.workplaceId==filter) && LocalDateTime.parse(it.date+"T"+it.start)>LocalDateTime.now()}.minByOrNull{it.date+it.start}
    var details by rememberSaveable { mutableStateOf(false) }
    Page {
        WorkplacePicker(vm){navigate("workplaces")}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){PurpleAction(L("+ New workplace","+ Neuer Arbeitsplatz"),Modifier.weight(1f)){navigate("new-workplace")};PurpleAction(L("Manage workplaces","Arbeitsplätze verwalten"),Modifier.weight(1f)){navigate("workplaces")}}
        WageCard(color=Lime) { Row(verticalAlignment=Alignment.CenterVertically) { Text(L("AVAILABLE AFTER EXPENSES","NACH AUSGABEN VERFÜGBAR"),Modifier.weight(1f),fontSize=10.sp,fontWeight=FontWeight.Bold); Text(L("ESTIMATE","GESCHÄTZT"),fontSize=9.sp,modifier=Modifier.border(1.dp,Ink.copy(alpha=.5f),RoundedCornerShape(20.dp)).padding(5.dp)) }
            Text(money(all.est.net-expenses.filter{it.date.startsWith(month)}.sumOf{it.amount}),fontSize=38.sp,fontWeight=FontWeight.Bold)
            Text(L("Includes planned shifts, minus monthly expenses","Mit geplanten Schichten, minus Monatsausgaben"),fontSize=12.sp)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { Surface(Modifier.weight(1f),shape=RoundedCornerShape(18.dp),color=Raised,contentColor=WhiteInk){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Eyebrow(L("Hours this month","Stunden dieses Monats"));Text(F.formatDuration(summary.workedMinutes),fontWeight=FontWeight.Bold)}}
            Surface(onClick={if(next==null)add()else edit(next.id)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(18.dp),color=Raised,contentColor=WhiteInk){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Eyebrow(L("Next shift","Nächste Schicht"));Text(next?.let{it.date.substring(5)+" · "+it.start} ?: L("Plan a shift →","Schicht planen →"),fontSize=13.sp,fontWeight=FontWeight.Bold)}} }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){PurpleAction(L("Budgets & goals ↗","Budgets & Sparziele ↗"),Modifier.weight(1f),"planning",true){navigate("planning")};PurpleAction(L("Reminders ↗","Erinnerungen ↗"),Modifier.weight(1f),"reminders",true){navigate("reminders")}}
        TimeClock(vm)
        WageCard(Modifier.testTag("bonus-card")) {
            Heading(L("Base pay & bonuses","Grundlohn & Zuschläge"))
            val ratio=if(summary.gross>0)(summary.baseGross/summary.gross).toFloat().coerceIn(0f,1f)else 0f
            Spacer(Modifier.height(2.dp))
            LinearProgressIndicator(progress={ratio},Modifier.fillMaxWidth().height(18.dp),color=Color(0xFF5831BD),trackColor=if(summary.gross>0)Pink else Color(0xFFE5DDED))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)){
                listOf(Triple(L("Base pay","Grundlohn"),summary.baseGross,Color(0xFF5831BD)),Triple(L("Bonuses","Zuschläge"),summary.bonus.total,Pink)).forEach{(label,amount,tint)->Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Box(Modifier.size(8.dp).background(tint,RoundedCornerShape(8.dp)));Text(label,fontSize=12.sp)};Text(money(amount),Modifier.padding(start=14.dp),fontSize=18.sp,fontWeight=FontWeight.Medium)}}
            }
            listOf(listOf(L("Overtime","Überstunden") to summary.bonus.overtime,L("Night work","Nachtarbeit") to summary.bonus.night),listOf(L("Sunday","Sonntag") to summary.bonus.sunday,L("Public holiday","Feiertag") to summary.bonus.holiday)).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{(label,amount)->Surface(Modifier.weight(1f),shape=RoundedCornerShape(14.dp),color=LocalContentColor.current.copy(alpha=.025f)){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(label,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.65f));Text(money(amount),fontSize=20.sp,color=Color(0xFF1B7650))}}}}}
            Text(L("Bonuses are planning estimates. Your employment or collective agreement takes precedence.","Zuschläge sind Planungsschätzungen. Dein Arbeits- oder Tarifvertrag ist maßgeblich."),fontSize=12.sp,color=LocalContentColor.current.copy(alpha=.65f))
        }

        Surface(onClick={details=!details},shape=RoundedCornerShape(18.dp),color=Purple,contentColor=WhiteInk,border=BorderStroke(1.dp,Lavender.copy(alpha=.4f))) {Text((if(details)"▾ " else "▸ ")+L("Earnings, progress & recent shifts","Verdienst, Fortschritt & letzte Schichten"),Modifier.fillMaxWidth().padding(18.dp),fontSize=13.sp,fontWeight=FontWeight.SemiBold)}
        if(details) {
            val previous=PayrollCalculator.summary(YearMonth.parse(month).minusMonths(1).toString(),shifts,settings,workplaces,filter)
            val delta=if(previous.gross>0)"%+.1f%%".format(F.locale,(summary.gross/previous.gross-1)*100)else "–"
            Row(Modifier.testTag("earnings-row"),horizontalArrangement=Arrangement.spacedBy(12.dp)) { Metric(L("Gross earned","Brutto verdient"),money(summary.gross),F.formatDuration(summary.minutes),Modifier.weight(1f),L("To date","Bisher"),summary.done.size.toString()+L(" shifts"," Schichten"));Metric(L("Estimated net","Geschätztes Netto"),money(summary.est.net),money(if(summary.minutes>0)summary.est.net/(summary.minutes/60.0)else 0.0)+L("/h net","/h netto"),Modifier.weight(1f)) }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { Metric(L("Monthly forecast","Monatsprognose"),money(forecast?.gross ?: 0.0),"~ "+money(PayrollCalculator.estimate(forecast?.gross ?: 0.0,settings).net)+L(" net"," netto"),Modifier.weight(1f),when(forecast?.confidence){"Hoch"->L("HIGH","HOCH");"Mittel"->L("MEDIUM","MITTEL");"Niedrig"->L("LOW","NIEDRIG");else->L("NO DATA","KEINE DATEN")},F.formatDuration(forecast?.minutes ?: 0));Metric(L("Vs previous month","Zum Vormonat"),delta,L("Previous month: ","Vormonat: ")+money(previous.gross),Modifier.weight(1f)) }
            WageCard(Modifier.testTag("progress-card")) { val progress=if(settings.target>0)(summary.workedMinutes/(settings.target*60)).toFloat().coerceIn(0f,1f)else 0f
                Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Eyebrow(L("Working time","Arbeitszeit"));Heading(L("Monthly progress","Monatsfortschritt"))};Text("${(progress*100).toInt()}%",fontSize=29.sp,color=Color(0xFF5831BD))}
                LinearProgressIndicator(progress={progress},Modifier.fillMaxWidth().height(10.dp),color=Color(0xFF5831BD));Stat(F.formatDuration(summary.workedMinutes)+L(" worked"," gearbeitet"),L("Target: ","Ziel: ")+F.formatDuration((settings.target*60).toInt()))
                val groups=summary.completed.groupBy{LocalDate.parse(it.date).with(java.time.DayOfWeek.MONDAY)}
                val ym=YearMonth.parse(month); val start=ym.atDay(1).with(java.time.DayOfWeek.MONDAY)
                val weeks=generateSequence(start){it.plusWeeks(1)}.takeWhile{it<=ym.atEndOfMonth()}.toList();val max=groups.values.maxOfOrNull{xs->xs.sumOf{it.minutes}}?.coerceAtLeast(1) ?: 1
                Row(Modifier.fillMaxWidth().height(140.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Bottom) {weeks.forEach { w->Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){Box(Modifier.fillMaxWidth(.7f).height((110f*(groups[w]?.sumOf{it.minutes} ?: 0)/max).coerceAtLeast(2f).dp).background(Brush.verticalGradient(listOf(Pink,Color(0xFF5831BD))),RoundedCornerShape(5.dp)));Text("W"+w.get(WeekFields.ISO.weekOfWeekBasedYear()),fontSize=11.sp)} } }
            }
            WageCard(Modifier.testTag("deductions-card")) { Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Eyebrow(L("Estimate","Schätzung"));Heading(L("Deductions overview","Abzüge im Überblick"))};Text(month.take(4),fontSize=12.sp,color=Color(0xFF5831BD),modifier=Modifier.background(Color(0xFFEADFFF),RoundedCornerShape(20.dp)).padding(8.dp))}
                val tax=summary.est
                if(tax.manualDeduction!=null)Stat(L("Manual deduction","Manueller Abzug"),"− "+money(tax.manualDeduction))else {
                    Stat(L("Wage tax","Lohnsteuer"),"− "+money(tax.wageTax))
                    Stat(L("Pension insurance","Rentenversicherung"),"− "+money(tax.pension))
                    Stat(L("Health insurance","Krankenversicherung"),"− "+money(tax.health))
                    Stat(L("Long-term care insurance","Pflegeversicherung"),"− "+money(tax.care))
                    Stat(L("Unemployment insurance","Arbeitslosenversicherung"),"− "+money(tax.unemployment))
                    Stat(L("Solidarity & church tax","Soli & Kirchensteuer"),"− "+money(tax.soli+tax.church))
                }
                HorizontalDivider(color=LocalContentColor.current.copy(alpha=.15f))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(L("Estimated net","Geschätztes Netto"),fontWeight=FontWeight.Bold);Text(money(tax.net),fontSize=24.sp,fontWeight=FontWeight.Bold,color=Color(0xFF1B7650))}
                Action(L("View calculation basis →","Berechnungsgrundlage ansehen →"),Modifier.fillMaxWidth()){navigate("pay")}
            }
            WageCard { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Eyebrow(L("Recent entries","Letzte Einträge"));Heading(L("Your shifts","Deine Schichten"))};TextButton({navigate("shifts")}){Text(L("Show all →","Alle →"))}};if(summary.list.isEmpty())Column(Modifier.fillMaxWidth().heightIn(min=120.dp).padding(top=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){Text(L("No shifts yet","Noch keine Schichten"),fontSize=18.sp);Text(L("Add your first work shift.","Trage deine erste Schicht ein."),fontSize=16.sp,color=LocalContentColor.current.copy(alpha=.65f))};summary.list.sortedByDescending{it.date+it.start}.take(5).forEach{shift->ShiftRow(shift,workplaces.firstOrNull{it.id==shift.workplaceId}?.name ?: "",{edit(shift.id)})} }
        }
        WageCard(color=Purple) { CompositionLocalProvider(LocalContentColor provides WhiteInk) {
            Eyebrow(L("Payslip check","Lohnabrechnung"));Heading(L("Compare your pay","Deinen Lohn vergleichen"))
            val pays=payslips.filter{it.month==month&&(filter=="all"||it.workplaceId==filter)}
            if(pays.isEmpty())Text(L("Add a payslip to see the difference.","Abrechnung hinzufügen und Unterschiede sehen."),fontSize=13.sp)
            else {
                val expected=pays.sumOf{PayrollCalculator.summary(month,shifts,settings,workplaces,it.workplaceId).est.net}
                val actual=pays.sumOf{it.actualNet}
                Stat(L("Estimated net","Geschätztes Netto"),money(expected));Stat(L("Payslip net","Abrechnungsnetto"),money(actual))
                Stat(L("Difference","Differenz"),money(actual-expected))
            }
            Action(L("View payslips →","Abrechnungen ansehen →")){navigate("history")}
        } }

    }
}
@Composable fun Metric(label:String,value:String,foot:String,modifier:Modifier=Modifier,badge:String="",trailingFoot:String="") {
    WageCard(modifier,padding=14.dp) {
        Column(Modifier.heightIn(min=100.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(32.dp),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(label.uppercase(),Modifier.weight(1f),fontSize=10.sp,fontWeight=FontWeight.SemiBold,lineHeight=14.sp)
                if(badge.isNotEmpty())Text(badge.uppercase(),fontSize=8.sp,lineHeight=10.sp,color=if(badge==L("To date","Bisher"))Color(0xFF5831BD)else Color(0xFF85590B),modifier=Modifier.background(if(badge==L("To date","Bisher"))Color(0xFFEADFFF)else Color(0xFFFFF0C6),RoundedCornerShape(20.dp)).padding(horizontal=5.dp,vertical=4.dp))
            }
            Text(value,fontSize=23.sp,fontWeight=FontWeight.Medium,maxLines=1)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(foot,Modifier.weight(1f),fontSize=10.sp,color=LocalContentColor.current.copy(alpha=.65f));if(trailingFoot.isNotEmpty())Text(trailingFoot,fontSize=10.sp,color=LocalContentColor.current.copy(alpha=.65f))}
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun ShiftRow(shift:Shift,place:String,click:()->Unit,modifier:Modifier=Modifier,longClick:(()->Unit)?=null) {Row(modifier.fillMaxWidth().combinedClickable(onClick=click,onLongClick=longClick).padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    val date=LocalDate.parse(shift.date);Column(Modifier.border(1.dp,MaterialTheme.colorScheme.outline,RoundedCornerShape(12.dp)).padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(date.dayOfMonth.toString(),fontSize=21.sp);Text(date.month.getDisplayName(java.time.format.TextStyle.SHORT,F.locale).uppercase(),fontSize=9.sp)}
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){Text(shift.note?.takeIf{it.isNotBlank()} ?: L("Work shift","Arbeitsschicht"),fontWeight=FontWeight.SemiBold,fontSize=15.sp);Text(place,fontSize=11.sp,maxLines=1);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Text(shift.start+"–"+shift.end,fontSize=11.sp);Text("${shift.breakMin} "+L("min break","Min. Pause"),fontSize=11.sp)};Text(when(shift.status){"planned"->L("Planned","Geplant");"cancelled"->L("Cancelled","Abgesagt");else->L("Completed","Erledigt")},fontSize=10.sp,color=MaterialTheme.colorScheme.primary)}
    Text("›",fontSize=20.sp)
} }
@Composable fun TimeClock(vm:MainViewModel) {
    val document by vm.document.collectAsState();val places by vm.workplaces.collectAsState();val filter by vm.workplaceFilter.collectAsState();val shifts by vm.shifts.collectAsState()
    val timer=remember(document){JSONObject(document).optJSONObject("activeTimer")};var now by remember{mutableLongStateOf(System.currentTimeMillis())};var confirm by remember{mutableStateOf("")}
    LaunchedEffect(timer?.optLong("startedAt")){while(true){now=System.currentTimeMillis();delay(1000)}}
    val totals=timer?.let{timerTotals(it,now)} ?: (0L to 0L);val breaking=timer?.optString("state")=="break"
    WageCard(color=Purple) {CompositionLocalProvider(LocalContentColor provides WhiteInk){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow(L("Live time clock","Live-Zeiterfassung"));if(timer!=null)Text(L("● Ongoing","● Laufend"),fontSize=11.sp,color=Lime)}
        Text(if(timer==null)L("● Ready to start","● Bereit zum Start")else if(breaking)L("On break","Pause läuft")else L("Working","Arbeitszeit läuft"),color=WhiteInk,fontSize=18.sp,fontWeight=FontWeight.SemiBold)
        Text(clock(if(breaking)totals.second else totals.first),fontSize=40.sp,fontWeight=FontWeight.Bold,modifier=Modifier.testTag("timer-value"))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(28.dp)){
            Column{Eyebrow(L("Start","Beginn"));Text(timer?.let{Instant.ofEpochMilli(it.optLong("startedAt")).atZone(ZoneId.systemDefault()).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))} ?: "–",fontSize=16.sp)}
            Column{Eyebrow(L("Break","Pause"));Text(clock(totals.second),fontSize=16.sp)}
        }
        if(timer==null)Action(L("▶ Start work","▶ Arbeit starten"),Modifier.fillMaxWidth(),true,places.isNotEmpty()){val wp=places.firstOrNull{it.id==filter} ?: places.first();val ts=System.currentTimeMillis();val linked=shifts.filter{it.status=="planned"&&it.workplaceId==wp.id&&PayrollCalculator.shiftRange(it).let{r->LocalDateTime.now() >= r.start.minusMinutes(30) && LocalDateTime.now() < r.end}}.singleOrNull();vm.updateDocument{it.put("activeTimer",JSONObject().put("startedAt",ts).put("segmentStartedAt",ts).put("state","working").put("workplaceId",wp.id).put("shiftId",linked?.id))}}
        else {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Action(if(breaking)L("Resume","Fortsetzen")else L("Take a break","Pause starten"),Modifier.weight(1f),true){vm.updateDocument{d->val t=d.optJSONObject("activeTimer") ?: return@updateDocument;val at=System.currentTimeMillis();val x=timerTotals(t,at);t.put("workedMs",x.first).put("breakMs",x.second).put("state",if(breaking)"working" else "break").put("segmentStartedAt",at).put("breakStartedAt",at)}};Action(L("Finish","Beenden"),Modifier.weight(1f)){confirm="finish"}};TextButton({confirm="discard"}){Text(L("Discard timer","Zeiterfassung verwerfen"),color=Lavender)}}
    } }
    if(confirm.isNotEmpty())AlertDialog(onDismissRequest={confirm=""},title={Text(if(confirm=="finish")L("Save timed shift?","Erfasste Schicht speichern?")else L("Discard timer?","Zeiterfassung verwerfen?"))},confirmButton={TextButton({if(confirm=="finish"&&timer!=null){val at=System.currentTimeMillis();val times=timerTotals(timer,at);val start=Instant.ofEpochMilli(timer.getLong("startedAt")).atZone(ZoneId.systemDefault());val end=Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault());val old=shifts.firstOrNull{it.id==timer.optString("shiftId")&&it.status=="planned"};val wp=places.firstOrNull{it.id==timer.optString("workplaceId")};val sh=(old ?: Shift(UUID.randomUUID().toString(),start.toLocalDate().toString(),"","",0)).copy(date=start.toLocalDate().toString(),start=start.toLocalTime().toString().take(5),end=end.toLocalTime().toString().take(5),minutes=kotlin.math.round(times.first/60000.0).toInt().coerceAtLeast(1),breakMin=kotlin.math.round(times.second/60000.0).toInt(),workplaceId=wp?.id ?: "default",wage=old?.wage ?: wp?.wage,status="completed",statusSource="manual",plannedDate=old?.plannedDate ?: old?.date,plannedStart=old?.plannedStart ?: old?.start,plannedEnd=old?.plannedEnd ?: old?.end,completedAutomatically=false);vm.saveTimedShift(sh)} else vm.updateDocument{it.put("activeTimer",JSONObject.NULL)};confirm=""}){Text(L("Confirm","Bestätigen"))}},dismissButton={TextButton({confirm=""}){Text(L("Cancel","Abbrechen"))}})
}
