package com.paydaytracker.app.ui.parity

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import com.paydaytracker.app.*
import com.paydaytracker.app.ui.MainViewModel
import kotlinx.coroutines.*
import org.json.JSONObject

@Composable fun BackupPage(vm:MainViewModel,onRestored:(()->Unit)?=null) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val auto=remember{AutoBackup.get(context)}
    var status by remember{mutableStateOf(JSONObject(auto.state()))};var message by remember{mutableStateOf("")};var pending by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){while(true){status=JSONObject(auto.state());delay(1000)}}
    val saved=L("Backup saved","Sicherung gespeichert");val restored=L("Backup restored","Sicherung wiederhergestellt");val failed=L("Could not read or write the file. Your existing records are unchanged.","Die Datei konnte nicht gelesen oder geschrieben werden. Vorhandene Daten bleiben erhalten.")
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)scope.launch{busy=true;message=try{withContext(Dispatchers.IO){val json=vm.repository.exportBackupJson();requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use{it.write(json.toByteArray())}};saved}catch(e:Exception){failed};busy=false}}
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{busy=true;try{val raw=withContext(Dispatchers.IO){requireNotNull(context.contentResolver.openInputStream(uri)).use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var n=input.read(buffer)
                    while(n>=0){require(out.size()+n<=8*1024*1024);out.write(buffer,0,n);n=input.read(buffer)}
                    out.toString("UTF-8")}};val root=JSONObject(raw);val data=root.optJSONObject("data") ?: root;require(data.has("settings")&&data.has("shifts"));pending=raw}catch(e:Exception){message=failed};busy=false}}
    val folder=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->if(result.resultCode==Activity.RESULT_OK)result.data?.let{intent->intent.data?.let{uri->auto.configure(uri,intent.flags);scope.launch{delay(300);NativeCoordinator.syncAutoBackup(context,vm.repository,scope)}}}}
    Page {
        WageCard{Heading(L("Automatic backup","Automatische Sicherung"));Text(when(status.optString("status")){"saved"->L("Up to date","Aktuell");"pending","saving"->L("Changes waiting to be saved","Änderungen werden gesichert");"error"->L("Backup needs attention","Sicherung prüfen");else->L("Choose a folder once","Einmal einen Ordner auswählen")});if(status.optString("folder").isNotEmpty())Text(status.optString("folder"),fontSize=12.sp);Text(L("Changes are saved automatically to a current backup and one previous copy.","Änderungen werden automatisch in einer aktuellen und einer vorherigen Sicherung gespeichert."),fontSize=12.sp)
            Action(L("Choose backup folder","Sicherungsordner wählen"),primary=true){folder.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION))}
            if(status.optBoolean("enabled")){Action(L("Back up now","Jetzt sichern")){NativeCoordinator.syncAutoBackup(context,vm.repository,scope);auto.flush()};TextButton({auto.disable()}){Text(L("Turn off automatic backup","Automatische Sicherung ausschalten"))}}
        }
        WageCard{Heading(L("Backup & restore","Sichern & wiederherstellen"));Action(L("Export backup","Sicherung exportieren"),enabled=!busy){export.launch("WageTrack-backup.json")};Action(L("Restore from file / Drive","Aus Datei / Drive wiederherstellen"),enabled=!busy){restore.launch(arrayOf("application/json","application/octet-stream","text/plain"))};Text(L("Choose Google Drive in the Android file picker to save or restore a cloud file.","Wähle Google Drive in der Android-Dateiauswahl, um eine Cloud-Datei zu sichern oder wiederherzustellen."),fontSize=12.sp)}
        WageCard{GoogleAccountPanel()}
        CsvExport(vm)
        if(message.isNotEmpty())Text(message,fontSize=13.sp)
    }
    if(pending!=null)AlertDialog(onDismissRequest={pending=null},title={Text(L("Restore this backup?","Diese Sicherung wiederherstellen?"))},text={Text(L("Matching records and settings will be updated. Other existing records are kept.","Übereinstimmende Datensätze und Einstellungen werden aktualisiert. Andere Datensätze bleiben erhalten."))},confirmButton={TextButton({val raw=pending!!;pending=null;scope.launch{busy=true;val ok=vm.repository.importBackupJson(raw);message=if(ok)restored else failed;if(ok){vm.updateDocument{it.put("onboardingCompleted",true)};onRestored?.invoke()};busy=false}}){Text(L("Restore","Wiederherstellen"))}},dismissButton={TextButton({pending=null}){Text(L("Cancel","Abbrechen"))}})
}
@Composable private fun CsvExport(vm: MainViewModel) {
    val context = LocalContext.current
    val shifts by vm.shifts.collectAsState()
    val places by vm.workplaces.collectAsState()
    val settings by vm.settings.collectAsState()
    val month by vm.selectedMonth.collectAsState()
    val filter by vm.workplaceFilter.collectAsState()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    val failed = L("Could not export CSV", "CSV-Export fehlgeschlagen")
    val saved = L("CSV saved", "CSV gespeichert")
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            message = try {
                withContext(Dispatchers.IO) {
                    val csv = com.paydaytracker.app.data.ShiftCsv.export(month, shifts, settings, places, filter)
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }
                saved
            } catch (e: Exception) { failed }
        }
    }
    WageCard {
        Heading(L("Monthly export", "Monatsexport"))
        Action(L("Export CSV", "CSV exportieren")) { export.launch("WageTrack-$month.csv") }
        if (message.isNotEmpty()) Text(message)
    }
}

@Composable fun GoogleAccountPanel(){
    val context=LocalContext.current
    var accountEvent by remember{mutableStateOf("")};val account=remember{GoogleAccount(context as Activity){accountEvent=it}}
    DisposableEffect(account){onDispose{account.destroy()}}
    val accountState=remember(accountEvent){JSONObject(account.state())}
Heading(L("Google account","Google-Konto"));if(accountState.optString("email").isNotEmpty()){Text(accountState.optString("email"));Action(L("Sign out","Abmelden")){account.signOut()}}else{Action(L("Sign in with Google","Mit Google anmelden"),enabled=!accountState.optBoolean("busy")){account.signIn()};if(!accountState.optBoolean("configured"))Text(L("Google sign-in is not configured in this build. File and folder backups work without sign-in.","Google-Anmeldung ist in diesem Build nicht eingerichtet. Datei- und Ordnersicherungen funktionieren ohne Anmeldung."),fontSize=12.sp)};if(accountEvent in listOf("error","verifyError"))Text(L("Sign-in failed. Please try again.","Anmeldung fehlgeschlagen. Bitte erneut versuchen."))
}
