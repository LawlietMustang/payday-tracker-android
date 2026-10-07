package com.paydaytracker.app.ui.parity

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.app.NotificationManagerCompat
import com.paydaytracker.app.R
import com.paydaytracker.app.data.*
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters as F
import kotlinx.coroutines.launch
import java.time.*
import java.util.Locale

@Composable fun SetupField(label:String,value:String,change:(String)->Unit,placeholder:String="",modifier:Modifier=Modifier) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,fontSize=15.sp,fontWeight=FontWeight.Medium)
        OutlinedTextField(value,change,Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(placeholder)},shape=RoundedCornerShape(14.dp))
    }
}
@Composable private fun SetupPicker(label:String,value:String,modifier:Modifier=Modifier,click:()->Unit) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,fontSize=15.sp,fontWeight=FontWeight.Medium)
        OutlinedButton(click,Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=LocalContentColor.current)) {Text(value,Modifier.weight(1f),textAlign=TextAlign.Start)}
    }
}
@Composable fun CountryChoice(value:String,change:(String)->Unit) {
    var open by remember{mutableStateOf(false)};var search by remember{mutableStateOf("")}
    val locale=F.locale
    SetupPicker(L("Country","Land"),if(value.isBlank())L("Choose your country","Land auswählen")else Locale("",value).getDisplayCountry(locale)){open=true}
    if(open)AlertDialog(onDismissRequest={open=false},title={Text(L("Country","Land"))},text={Column{
        Field(L("Search country","Land suchen"),search,{search=it})
        androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=320.dp)) {
            val countries=Locale.getISOCountries().map{it to Locale("",it).getDisplayCountry(locale)}.filter{it.second.contains(search,true)||it.first.equals(search,true)}.sortedBy{it.second}
            items(countries.size){i->val (code,name)=countries[i];TextButton({change(code);open=false}){Text(name)}}
        }
    }},confirmButton={TextButton({open=false}){Text(L("Close","Schließen"))}})
}
@Composable fun Onboarding(vm:MainViewModel){
    var step by rememberSaveable{mutableIntStateOf(0)}
    var restore by rememberSaveable{mutableStateOf(false)}
    var name by rememberSaveable{mutableStateOf("")};var country by rememberSaveable{mutableStateOf("")}
    var job by rememberSaveable{mutableStateOf("")};var wage by rememberSaveable{mutableStateOf("")};var target by rememberSaveable{mutableStateOf("")}
    var currency by rememberSaveable{mutableStateOf("EUR")};var taxMode by rememberSaveable{mutableStateOf("manual")};var taxClass by rememberSaveable{mutableStateOf("I")};var deduction by rememberSaveable{mutableStateOf("0")}
    var firstShift by rememberSaveable{mutableStateOf(true)};var start by rememberSaveable{mutableStateOf("09:00")};var end by rememberSaveable{mutableStateOf("17:00")};var nextDay by rememberSaveable{mutableStateOf(false)}
    var pause by rememberSaveable{mutableStateOf("0")};var date by rememberSaveable{mutableStateOf(LocalDate.now().toString())}
    var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};val scope=rememberCoroutineScope();val context=LocalContext.current
    var allowed by remember{mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){allowed=NotificationManagerCompat.from(context).areNotificationsEnabled()}
    val resumeOwner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(resumeOwner){val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME)allowed=NotificationManagerCompat.from(context).areNotificationsEnabled()};resumeOwner.lifecycle.addObserver(observer);onDispose{resumeOwner.lifecycle.removeObserver(observer)}}
    val paidMinutes=runCatching {LocalDate.parse(date);val a=LocalTime.parse(start);val b=LocalTime.parse(end);val duration=Duration.between(a,b).toMinutes()+if(nextDay)1440 else 0;val breakMin=pause.toInt();require(duration in 1..1440&&breakMin>=0&&breakMin<duration);(duration-breakMin).toInt()}.getOrNull()
    BackHandler(restore||step>0){if(restore)restore=false else{error="";step--}}
    Surface(Modifier.fillMaxSize(),color=Ink,contentColor=WhiteInk){Column(Modifier.safeDrawingPadding().imePadding().padding(horizontal=20.dp,vertical=16.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Image(painterResource(R.drawable.app_icon),null,Modifier.size(34.dp));Spacer(Modifier.width(12.dp));Text("WageTrack",fontSize=19.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));val lang=LocalLanguage.current;Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){TextButton({vm.updateDocument{it.put("language",if(lang=="en")"de"else "en")}}){Text(if(lang=="en")"English"else "Deutsch",color=MaterialTheme.colorScheme.onSurface)}}}
        if(restore){Box(Modifier.weight(1f)){BackupPage(vm){restore=false}};Action(L("Back","Zurück")){restore=false};return@Column}
        Row(Modifier.padding(top=18.dp,bottom=18.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){repeat(4){Box(Modifier.weight(1f).height(4.dp).background(if(it<=step)Lime else Raised,RoundedCornerShape(3.dp)))}}
        key(step){Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("setup-step-$step"),verticalArrangement=Arrangement.spacedBy(18.dp)){
            Eyebrow(L("Step ${step+1} / 4","Schritt ${step+1} / 4"))
            when(step){
                0->{
                    Text(L("Your time. Your earnings.","Deine Zeit. Dein Verdienst."),fontSize=27.sp,fontWeight=FontWeight.SemiBold)
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){Image(painterResource(R.drawable.app_icon),null,Modifier.size(42.dp));Text(L("Track your hours. Understand your pay.","Erfasse deine Stunden. Verstehe deinen Lohn."),fontSize=17.sp,color=Lavender)}
                    WageCard {SetupField(L("What is your name?","Wie heißt du?"),name,{name=it},L("Your name","Dein Name"));CountryChoice(country){country=it;taxMode=if(it=="DE")"germany"else "manual"}}
                    Text(L("No account needed. You can change these details later.","Kein Konto nötig. Du kannst diese Angaben später ändern."),fontSize=13.sp,color=Lavender,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
                }
                1->{
                    Heading(L("Your workplace","Dein Arbeitsplatz"));Text(L("Start with one workplace. You can add more later.","Starte mit einem Arbeitsplatz. Weitere kannst du später hinzufügen."),color=Lavender,fontSize=14.sp)
                    WageCard {
                        SetupField(L("Workplace name","Name des Arbeitsplatzes"),job,{job=it},L("e.g. Burger King","z. B. Burger King"))
                        CurrencyChoice(currency){currency=it};HorizontalDivider()
                        SetupField(L("Gross hourly wage","Bruttostundenlohn")+" ($currency)",wage,{wage=it},L("e.g. 14.50","z. B. 14,50"))
                        SetupField(L("Monthly hours target","Monatsstundenziel"),target,{target=it},L("e.g. 80","z. B. 80"))
                    }
                    WageCard {
                        CountryChoice(country){country=it;taxMode=if(it=="DE")"germany"else "manual"}
                        if(country=="DE"){
                            Text(L("Tax class","Steuerklasse"),fontSize=15.sp,fontWeight=FontWeight.Medium)
                            listOf(listOf("I","II","III"),listOf("IV","V","VI")).forEach{classes->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){classes.forEach{c->Action(c,Modifier.weight(1f),primary=taxClass==c){taxClass=c}}}}
                        }
                        Choice(L("Calculation","Berechnung"),taxMode,if(country=="DE")listOf("germany" to L("German estimate","Deutsche Schätzung"),"manual" to L("Manual deduction","Manueller Abzug"))else listOf("manual" to L("Manual deduction","Manueller Abzug"))){taxMode=it}
                        SetupField(L("Manual deductions (%)","Manuelle Abzüge (%)"),deduction,{deduction=it;if((it.replace(',','.').toDoubleOrNull() ?: 0.0)>0)taxMode="manual"})
                        Text(L("Simplified estimate. Enter a manual rate to use it instead.","Vereinfachte Schätzung. Alternativ einen manuellen Satz eingeben."),fontSize=13.sp,color=LocalContentColor.current.copy(alpha=.65f))
                    }
                    Text(L("One currency for all workplaces. No automatic conversion.","Eine Währung für alle Arbeitsplätze. Keine automatische Umrechnung."),fontSize=13.sp,color=Lavender)
                }
                2->{
                    Heading(L("Log your first shift","Erste Schicht eintragen"))
                    WageCard {
                        SetupPicker(L("Date","Datum"),runCatching{LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))}.getOrDefault(date)) {val d=LocalDate.parse(date);DatePickerDialog(context,{_,y,m,day->date=LocalDate.of(y,m+1,day).toString()},d.year,d.monthValue-1,d.dayOfMonth).show()}
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            SetupPicker(L("Start","Beginn"),start,Modifier.weight(1f)){val t=LocalTime.parse(start);TimePickerDialog(context,{_,h,m->start="%02d:%02d".format(Locale.ROOT,h,m)},t.hour,t.minute,true).show()}
                            SetupPicker(L("End","Ende"),end,Modifier.weight(1f)){val t=LocalTime.parse(end);TimePickerDialog(context,{_,h,m->end="%02d:%02d".format(Locale.ROOT,h,m)},t.hour,t.minute,true).show()}
                        }
                        Row(Modifier.fillMaxWidth().clickable{nextDay=!nextDay},verticalAlignment=Alignment.CenterVertically){Text(L("Ends next day","Endet am nächsten Tag"),Modifier.weight(1f));Checkbox(nextDay,{nextDay=it})}
                        SetupField(L("Unpaid break (minutes)","Unbezahlte Pause (Minuten)"),pause,{pause=it})
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(0,15,30,45).forEach{m->Action("$m min",Modifier.weight(1f),primary=pause==m.toString(),compact=true){pause=m.toString()}}}
                    }
                    WageCard(color=Raised){CompositionLocalProvider(LocalContentColor provides WhiteInk){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(L("Paid time","Bezahlte Zeit"),fontSize=13.sp);Text(paidMinutes?.let(F::formatDuration) ?: "–",fontSize=23.sp)};Column(horizontalAlignment=Alignment.End){Text(L("Base gross pay","Bruttogrundlohn"),fontSize=13.sp);Text(F.formatMoney((paidMinutes ?: 0)/60.0*(wage.replace(',','.').toDoubleOrNull() ?: 0.0),currency),fontSize=23.sp)}}}}
                    Action(L("Skip this step","Diesen Schritt überspringen"),Modifier.fillMaxWidth()){firstShift=false;step=3;error=""}
                }
                3->{
                    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Surface(shape=RoundedCornerShape(24.dp),color=Raised,border=BorderStroke(1.dp,Lavender.copy(alpha=.3f))){Box(Modifier.size(80.dp),contentAlignment=Alignment.Center){Mark("reminders",Modifier.size(40.dp),Lime)}}}
                    Text(L("Allow notifications to get reminders for your planned shifts.","Erlaube Benachrichtigungen für deine geplanten Schichten."),fontSize=19.sp,color=Lavender,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
                    WageCard {
                        Heading(L("Choose when to be reminded","Wähle deinen Erinnerungszeitpunkt"))
                        Text(L("In Reminders, choose all upcoming shifts and set how many hours or days before they start.","Wähle in Erinnerungen alle bevorstehenden Schichten und wie viele Stunden oder Tage vorher du erinnert werden möchtest."),fontSize=14.sp,color=LocalContentColor.current.copy(alpha=.65f))
                        Action(if(allowed)L("Notifications allowed","Benachrichtigungen erlaubt")else L("Allow notifications","Benachrichtigungen erlauben"),Modifier.fillMaxWidth(),primary=true){if(Build.VERSION.SDK_INT>=33&&!allowed)permission.launch(Manifest.permission.POST_NOTIFICATIONS)else{context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName))}}
                        Text(if(allowed)L("Notifications are enabled.","Benachrichtigungen sind aktiviert.")else L("Not allowed yet. If Android does not ask, check app notifications in phone settings.","Noch nicht erlaubt. Falls Android nicht fragt, prüfe die App-Benachrichtigungen in den Einstellungen."),fontSize=14.sp)
                        Text(L("Optional. You can use the app without notifications.","Optional. Du kannst die App ohne Benachrichtigungen nutzen."),fontSize=13.sp,color=LocalContentColor.current.copy(alpha=.65f))
                    }
                }
            }
            if(error.isNotEmpty())Text(error,color=Lime)
            Spacer(Modifier.height(8.dp))
        }}
        Column(Modifier.padding(top=10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            HorizontalDivider(color=Lavender.copy(alpha=.3f))
            if(step==0)Action(L("Restore a backup","Sicherung wiederherstellen"),Modifier.fillMaxWidth()){restore=true}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                if(step>0)Action(L("Back","Zurück"),Modifier.widthIn(min=90.dp),enabled=!busy){step--;error=""}
                val invalid=L("Check the required fields and times.","Pflichtfelder und Uhrzeiten prüfen.")
                Action(if(step==0)L("Get started →","Los geht’s →")else if(step==3)L("Continue to app →","Zur App →")else L("Next →","Weiter →"),if(step==0)Modifier.fillMaxWidth()else Modifier.width(if(step==3)168.dp else 120.dp).testTag("setup-next"),primary=true,enabled=!busy,compact=step==3){
                    val w=wage.replace(',','.').toDoubleOrNull();val t=target.replace(',','.').toDoubleOrNull();val d=deduction.replace(',','.').toDoubleOrNull()
                    val valid=w!=null&&w.isFinite()&&w>0&&t!=null&&t.isFinite()&&t>0&&job.isNotBlank()&&d!=null&&d.isFinite()&&d in 0.0..100.0
                    if(step==1&&!valid||step==2&&paidMinutes==null)error=invalid else if(step<3){if(step==2)firstShift=true;error="";step++}else if(valid){busy=true;scope.launch{try{
                        val initial=if(firstShift&&paidMinutes!=null)Shift(java.util.UUID.randomUUID().toString(),date,start,end,paidMinutes,pause.toInt(),w,status=if(LocalDateTime.parse(date+"T"+start)>LocalDateTime.now())"planned"else "completed",statusSource="auto")else null
                        vm.repository.completeSetup(AppSettings(wage=w!!,target=t!!,currency=currency,taxMode=taxMode,taxclass=taxClass,deductionPercent=d!!),name.trim(),job.trim(),initial,country)
                    }catch(e:Exception){error=invalid};busy=false}}
                }
            }
        }
    }}
}
