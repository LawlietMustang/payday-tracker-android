package com.paydaytracker.app.ui.parity

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import com.paydaytracker.app.R
import com.paydaytracker.app.data.*
import com.paydaytracker.app.ui.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable fun Onboarding(vm:MainViewModel){
    var step by rememberSaveable{mutableIntStateOf(0)};var restore by rememberSaveable{mutableStateOf(false)};var name by rememberSaveable{mutableStateOf("")};var job by rememberSaveable{mutableStateOf("")};var wage by rememberSaveable{mutableStateOf("15")};var target by rememberSaveable{mutableStateOf("160")};var currency by rememberSaveable{mutableStateOf("EUR")};var taxMode by rememberSaveable{mutableStateOf("germany")};var firstShift by rememberSaveable{mutableStateOf(false)};var start by rememberSaveable{mutableStateOf("09:00")};var end by rememberSaveable{mutableStateOf("17:00")};var pause by rememberSaveable{mutableStateOf("30")};var date by rememberSaveable{mutableStateOf(LocalDate.now().toString())};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};val scope=rememberCoroutineScope();val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    BackHandler(restore||step>0){if(restore)restore=false else step--}
    Surface(Modifier.fillMaxSize(),color=Ink,contentColor=WhiteInk){Column(Modifier.safeDrawingPadding().padding(18.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Image(painterResource(R.drawable.app_icon),null,Modifier.size(38.dp));Spacer(Modifier.width(10.dp));Heading("WageTrack");Spacer(Modifier.weight(1f));val lang=LocalLanguage.current;TextButton({vm.updateDocument{it.put("language",if(lang=="en")"de"else "en")}}){Text(if(lang=="en")"English ⌄"else "Deutsch ⌄",color=WhiteInk)}}
        if(restore){Box(Modifier.weight(1f)){BackupPage(vm){restore=false}};Action(L("Back","Zurück")){restore=false};return@Column}
        Row(Modifier.padding(vertical=18.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){repeat(4){Box(Modifier.weight(1f).height(4.dp).background(if(it<=step)Lime else Raised))}}
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Eyebrow(L("Step ${step+1} / 4","Schritt ${step+1} / 4"))
            when(step){
                0->{Heading(L("Your time. Your earnings.","Deine Zeit. Dein Verdienst."));Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Image(painterResource(R.drawable.app_icon),null,Modifier.size(76.dp))};Text(L("Track your hours. Understand your pay. Start simple.","Erfasse deine Stunden. Verstehe deinen Lohn. Starte einfach."),fontSize=16.sp);listOf(L("Shifts & breaks","Schichten & Pausen") to L("Clock in or add hours manually","Einstempeln oder Stunden manuell eintragen"),L("Earnings at a glance","Verdienst auf einen Blick") to L("Gross pay, estimates and monthly overview","Bruttolohn, Schätzungen und Monatsübersicht"),L("All your workplaces","Alle deine Arbeitsplätze") to L("Each job with its own hourly wage","Jeder Job mit eigenem Stundenlohn")).forEach{(title,sub)->Column{Text(title);Text(sub,fontSize=12.sp,color=Lavender)}};Text(L("No account needed. Your data stays on your device.","Kein Konto nötig. Deine Daten bleiben auf deinem Gerät."),fontSize=12.sp,color=Lavender)}
                1->{Heading(L("Set up your job","Richte deinen Job ein"));WageCard{Field(L("Your name (optional)","Dein Name (optional)"),name,{name=it});Field(L("Workplace name","Name des Arbeitsplatzes"),job,{job=it});CurrencyChoice(currency){currency=it};Field(L("Gross hourly wage","Bruttostundenlohn")+" ($currency)",wage,{wage=it});Field(L("Monthly hours target","Monatsstundenziel"),target,{target=it});Choice(L("Net pay estimate","Nettoschätzung"),taxMode,listOf("germany" to L("Germany","Deutschland"),"manual" to L("Manual deduction","Manueller Abzug"))){taxMode=it}}}
                2->{Heading(L("Log your first shift","Erste Schicht eintragen"));WageCard{Toggle(L("Add a shift now","Jetzt eine Schicht hinzufügen"),checked=firstShift){firstShift=it};if(firstShift){Field(L("Date (YYYY-MM-DD)","Datum (JJJJ-MM-TT)"),date,{date=it});Field(L("Start (HH:mm)","Beginn (HH:mm)"),start,{start=it});Field(L("End (HH:mm)","Ende (HH:mm)"),end,{end=it});Field(L("Unpaid break (minutes)","Pause (Minuten)"),pause,{pause=it})}else Text(L("You can add shifts anytime.","Du kannst jederzeit Schichten hinzufügen."))}}
                3->{Heading(L("Stay on top of every shift","Behalte jede Schicht im Blick"));WageCard{Text(L("Allow notifications for upcoming shifts and work-hour reminders.","Erlaube Benachrichtigungen für bevorstehende Schichten und Stundenerinnerungen."));Action(L("Allow notifications","Benachrichtigungen erlauben"),primary=true){if(Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS)}}}
            };if(error.isNotEmpty())Text(error,color=Lime)
        }
        Column(Modifier.padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(step==0)Action(L("Restore a backup","Sicherung wiederherstellen"),Modifier.fillMaxWidth()){restore=true}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                if(step>0)Action(L("Back","Zurück"),Modifier.weight(1f),enabled=!busy){step--}
                val invalid=L("Check the required fields and times.","Pflichtfelder und Uhrzeiten prüfen.")
                Action(if(step==0)L("Get started →","Los geht’s →")else if(step==3)L("Continue to app →","Zur App →")else L("Next →","Weiter →"),Modifier.weight(1f),true,!busy){
                    val w=wage.replace(',','.').toDoubleOrNull();val t=target.replace(',','.').toDoubleOrNull()
                    val valid=w!=null&&w.isFinite()&&w>0&&t!=null&&t.isFinite()&&t>0&&job.isNotBlank()
                    val mins=runCatching{LocalDate.parse(date);val a=java.time.LocalTime.parse(start);val b=java.time.LocalTime.parse(end);val m=java.time.Duration.between(a,b).toMinutes().let{if(it<=0)it+1440 else it};val p=pause.toInt();require(p>=0&&p<m);(m-p).toInt()}.getOrNull()
                    if(step==1&&!valid||step==2&&firstShift&&mins==null)error=invalid else if(step<3){error="";step++}else if(valid){busy=true;scope.launch{try{val initial=if(firstShift&&mins!=null)Shift(java.util.UUID.randomUUID().toString(),date,start,end,mins,pause.toInt(),w,status=if(java.time.LocalDateTime.parse(date+"T"+start)>java.time.LocalDateTime.now())"planned"else "completed",statusSource="auto")else null;vm.repository.completeSetup(AppSettings(wage=w!!,target=t!!,currency=currency,taxMode=taxMode),name.trim(),job.trim(),initial)}catch(e:Exception){error=invalid};busy=false}}
                }
            }
        }
    }}
}
