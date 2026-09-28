package com.paydaytracker.app.ui.parity

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import com.paydaytracker.app.*
import com.paydaytracker.app.ui.MainViewModel
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONArray

@Composable fun DevicePage(vm:MainViewModel,route:String) {
    val context=LocalContext.current;val document by vm.document.collectAsState();val settings by vm.settings.collectAsState();val shifts by vm.shifts.collectAsState();val config=JSONObject(document).optJSONObject("shiftReminders") ?: JSONObject();val prefs=context.getSharedPreferences("device",0)
    var tab by remember(route){mutableStateOf(if(route=="lock")"lock"else "reminders")};var editor by remember{mutableStateOf(false)};var logging by remember{mutableStateOf(prefs.getBoolean("reminder",false))};var days by remember{mutableIntStateOf(prefs.getInt("reminderDays",62))};var hour by remember{mutableIntStateOf(prefs.getInt("reminderHour",20))};var minute by remember{mutableIntStateOf(prefs.getInt("reminderMinute",0))};var logOptions by remember{mutableStateOf(false)};var diagnostics by remember{mutableStateOf(false)};var locked by remember{mutableStateOf(prefs.getBoolean("lock",false))}
    LaunchedEffect(Unit){while(true){locked=prefs.getBoolean("lock",false);delay(1000)}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ShiftReminders.reconcile(context);ReminderReceiver.schedule(context)}
    fun requestNotifications(){if(Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS)}
    fun saveLog(){prefs.edit().putBoolean("reminder",logging).putInt("reminderDays",days).putInt("reminderHour",hour).putInt("reminderMinute",minute).apply();ReminderReceiver.schedule(context);vm.updateDocument{it.put("loggingReminder",JSONObject().put("enabled",logging).put("days",days).put("hour",hour).put("minute",minute))}}
    Page{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("lock" to L("App lock","App-Sperre"),"reminders" to L("Reminders","Erinnerungen"),"widget" to L("Widget","Widget")).forEach{(key,label)->Action(label,Modifier.weight(1f),primary=key==tab,compact=true){tab=key}}}
        when(tab){
            "reminders"->{Text(L("Shifts","Schichten"),color=Lavender);WageCard{Toggle(L("Upcoming shifts","Bevorstehende Schichten"),L("A reminder before you start","Vor Schichtbeginn erinnern"),config.optBoolean("enabled")){enabled->vm.updateDocument{val c=it.optJSONObject("shiftReminders") ?: JSONObject();c.put("enabled",enabled);it.put("shiftReminders",c)};if(enabled)requestNotifications()};HorizontalDivider();LinkRow(L("Timing & shifts","Zeitpunkt & Schichten"),"${config.optInt("amount",1)} "+(if(config.optString("unit")=="days")L("days before","Tage vorher")else L("hours before","Stunden vorher"))+" · "+(if(config.optString("scope","all")=="all")L("All shifts","Alle Schichten")else L("Selected shifts","Ausgewählte Schichten"))){editor=true}}
                Text(L("Work hours","Arbeitszeit"),color=Lavender);WageCard{Toggle(L("Log work hours","Stunden eintragen"),L("A nudge to record your hours","An das Eintragen erinnern"),logging){logging=it;saveLog();if(it)requestNotifications()};HorizontalDivider();LinkRow(L("Time & days","Uhrzeit & Tage"),"%02d:%02d".format(hour,minute)){logOptions=true}}
                WageCard{Toggle(L("Payslip reminder","Abrechnungserinnerung"),L("Check last month's pay","Lohn des Vormonats prüfen"),settings.payslipReminder){vm.saveSettings(settings.copy(payslipReminder=it));if(it)requestNotifications()}}
                TextButton({diagnostics=true}){Mark("info");Spacer(Modifier.width(8.dp));Text(L("Notification status","Benachrichtigungsstatus"),color=Lavender)}
            }
            "lock"->WageCard{Heading(L("App lock","App-Sperre"));Toggle(L("Biometric / PIN lock","Biometrie / PIN-Sperre"),L("Protect your records","Schütze deine Daten"),locked){MainActivity.appLockInstance?.change(it)};Text(L("Your device biometrics or your WageTrack PIN unlock the app. Widget amounts stay hidden while app lock is enabled.","Entsperre mit Biometrie oder deiner WageTrack-PIN. Bei aktivierter Sperre bleiben Widget-Beträge verborgen."),fontSize=12.sp)}
            "widget"->WageCard{Heading(L("Home-screen widget","Startbildschirm-Widget"));Text(L("See your work time and next shift at a glance.","Arbeitszeit und nächste Schicht auf einen Blick."),fontSize=13.sp);val fallback=L("Long-press your home screen, choose Widgets, then WageTrack.","Startbildschirm gedrückt halten, Widgets und dann WageTrack wählen.");Action(L("Add widget","Widget hinzufügen"),primary=true){val manager=AppWidgetManager.getInstance(context);if(manager.isRequestPinAppWidgetSupported)manager.requestPinAppWidget(ComponentName(context,PaydayWidget::class.java),null,null)else Toast.makeText(context,fallback,Toast.LENGTH_LONG).show()}}
        }
    }
    if(editor){var amount by remember{mutableStateOf(config.optInt("amount",1).toString())};var unit by remember{mutableStateOf(config.optString("unit","hours"))};var scope by remember{mutableStateOf(config.optString("scope","all"))};var ids by remember{mutableStateOf(config.optJSONArray("ids")?.let{a->(0 until a.length()).map{a.getString(it)}.toSet()} ?: emptySet())};val n=amount.toIntOrNull();AlertDialog(onDismissRequest={editor=false},title={Text(L("Upcoming shifts","Bevorstehende Schichten"))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){Field(L("Remind me","Erinnern"),amount,{amount=it});Choice(L("Before shift starts","Vor Schichtbeginn"),unit,listOf("hours" to L("Hours before","Stunden vorher"),"days" to L("Days before","Tage vorher"))){unit=it};Choice(L("Apply to","Anwenden auf"),scope,listOf("all" to L("All upcoming shifts","Alle bevorstehenden Schichten"),"selected" to L("Selected shifts","Ausgewählte Schichten"))){scope=it};if(scope=="selected")shifts.filter{it.status=="planned"}.forEach{s->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(s.id in ids,{ids=if(it)ids+s.id else ids-s.id});Text(s.date+" · "+s.start,fontSize=12.sp)}}}},confirmButton={TextButton({vm.updateDocument{it.put("shiftReminders",JSONObject().put("enabled",config.optBoolean("enabled")).put("amount",n).put("unit",unit).put("scope",scope).put("ids",JSONArray(ids.toList())))};editor=false},enabled=n!=null&&n in 1..(if(unit=="days")30 else 720)&&(scope=="all"||ids.isNotEmpty())){Text(L("Save","Speichern"))}},dismissButton={TextButton({editor=false}){Text(L("Cancel","Abbrechen"))}})}
    if(logOptions)AlertDialog(onDismissRequest={logOptions=false},title={Text(L("Time & days","Uhrzeit & Tage"))},text={Column{Action("%02d:%02d".format(hour,minute)){TimePickerDialog(context,{_,h,m->hour=h;minute=m},hour,minute,true).show()};(1..7).forEach{i->val bit=if(i==7)0 else i;val label=java.time.DayOfWeek.of(i).getDisplayName(java.time.format.TextStyle.FULL,com.paydaytracker.app.ui.common.Formatters.locale);Row(verticalAlignment=Alignment.CenterVertically){Checkbox(days and (1 shl bit)!=0,{days=if(it)days or (1 shl bit)else days and (1 shl bit).inv()});Text(label)}}}},confirmButton={TextButton({saveLog();logOptions=false},enabled=days!=0){Text(L("Save","Speichern"))}},dismissButton={TextButton({logOptions=false}){Text(L("Cancel","Abbrechen"))}})
    if(diagnostics){val status=ShiftReminders.status(context);AlertDialog(onDismissRequest={diagnostics=false},title={Text(L("Notification status","Benachrichtigungsstatus"))},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(if(status.optBoolean("allowed"))L("Notifications allowed","Benachrichtigungen erlaubt")else L("Notifications blocked","Benachrichtigungen blockiert"));Text(L("Scheduled shifts: ","Geplante Erinnerungen: ")+status.optInt("count"));Action(L("Notification settings","Benachrichtigungseinstellungen")){context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))};if(Build.VERSION.SDK_INT>=31)Action(L("Allow precise reminders","Genaue Erinnerungen erlauben")){context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+context.packageName)))};val sent=L("Test notification sent","Testbenachrichtigung gesendet");val blocked=L("Enable notifications in Android settings","Benachrichtigungen in Android erlauben");Action(L("Send test","Test senden")){Toast.makeText(context,if(ShiftReminders.test(context))sent else blocked,Toast.LENGTH_LONG).show()}}},confirmButton={TextButton({diagnostics=false}){Text(L("Close","Schließen"))}})}
}
