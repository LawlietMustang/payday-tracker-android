package com.paydaytracker.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.paydaytracker.app.R
import com.paydaytracker.app.AutoBackup
import com.paydaytracker.app.data.*
import com.paydaytracker.app.ui.common.Formatters
import com.paydaytracker.app.ui.parity.*
import com.paydaytracker.app.ui.theme.WageTrackTheme
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun MainApp(viewModel: MainViewModel) {
    val vm = viewModel
    val settings by vm.settings.collectAsState(); val document by vm.document.collectAsState()
    val ready by vm.ready.collectAsState(); val error by vm.startupError.collectAsState()
    val places by vm.workplaces.collectAsState(); val shifts by vm.shifts.collectAsState()
    val doc = remember(document) { JSONObject(document) }
    val context = LocalContext.current
    val language = doc.optString("language", context.getSharedPreferences("device", 0).getString("language", "de"))
    val dark = settings.theme == "dark" || settings.theme == "system" && isSystemInDarkTheme()
    SideEffect { Formatters.language = language; context.getSharedPreferences("device",0).edit().putString("language",language).apply() }
    var history by rememberSaveable { mutableStateOf(listOf("dashboard")) }
    val route = history.last()
    fun navigate(to: String) { if (to != route) history = history + to }
    fun back() { history = if (history.size > 1) history.dropLast(1) else listOf("dashboard") }
    var shiftOpen by remember { mutableStateOf(false) }; var editShift by remember { mutableStateOf<Shift?>(null) }
    var shiftDate by remember { mutableStateOf(LocalDate.now().toString()) }; var template by remember { mutableStateOf<ShiftTemplate?>(null) }
    fun add(date: String = LocalDate.now().toString(), t: ShiftTemplate? = null) { editShift=null; shiftDate=date; template=t; shiftOpen=true }
    fun edit(id: String) { editShift=shifts.firstOrNull { it.id==id }; template=null; shiftOpen=true }
    val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope()
    val onboard = !doc.optBoolean("onboardingCompleted")
    CompositionLocalProvider(LocalLanguage provides language) { WageTrackTheme(darkTheme = dark) {
        if (!ready) { Surface(Modifier.fillMaxSize(), color=Ink, contentColor=WhiteInk) { Column(Modifier.safeDrawingPadding().padding(32.dp), verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Heading("WageTrack"); Text(error ?: L("Loading your records…", "Deine Daten werden geladen…"))
            if(error != null) Action(L("Retry", "Erneut versuchen"),primary=true) { vm.initialize() }
        } }; return@WageTrackTheme }
        if (onboard) { Onboarding(vm); return@WageTrackTheme }
        BackHandler(drawer.isOpen || route != "dashboard") { if(drawer.isOpen) scope.launch { drawer.close() } else back() }
        val labels = mapOf("dashboard" to L("Your overview","Deine Übersicht"), "shifts" to L("Shift calendar","Schichtkalender"), "expenses" to L("Expenses","Ausgaben"), "history" to L("Monthly history","Monatsverlauf"), "settings" to L("Settings","Einstellungen"), "profile" to L("Profile","Profil"), "pay" to L("Pay & tax","Lohn & Steuern"), "planning" to L("Budgets & goals","Budgets & Sparziele"), "workplaces" to L("Workplaces","Arbeitsplätze"), "reminders" to L("Reminders & widget","Erinnerungen & Widget"), "lock" to L("App lock","App-Sperre"), "backup" to L("Backup & restore","Sichern & wiederherstellen"), "appearance" to L("App appearance","App-Darstellung"))
        ModalNavigationDrawer(drawerState=drawer, drawerContent={ ModalDrawerSheet(drawerContainerColor=Purple,drawerContentColor=WhiteInk) {
            Row(Modifier.padding(24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) { Image(painterResource(R.drawable.app_icon),null,Modifier.size(42.dp)); Heading("WageTrack") }
            listOf("dashboard","shifts","expenses","history","settings").forEach { id -> NavigationDrawerItem(label={Text(labels[id]!!)},selected=route==id,onClick={navigate(id);scope.launch{drawer.close()}}, modifier=Modifier.padding(horizontal=12.dp), colors=NavigationDrawerItemDefaults.colors(unselectedContainerColor=Purple,unselectedTextColor=WhiteInk,selectedContainerColor=Lime,selectedTextColor=Ink)) }
            Spacer(Modifier.weight(1f))
            val backup = JSONObject(AutoBackup.get(context).state())
            Column(Modifier.padding(24.dp)) { LinkRow(if(backup.optString("status")=="saved") L("Backed up","Gesichert") else L("Manage backup","Sicherung verwalten"), icon="backup") { navigate("backup");scope.launch{drawer.close()} }; Text("WageTrack ${com.paydaytracker.app.BuildConfig.VERSION_NAME}",fontSize=11.sp,color=Lavender) }
        } }) {
            Scaffold(containerColor=MaterialTheme.colorScheme.background, contentColor=WhiteInk,
                contentWindowInsets=WindowInsets.safeDrawing,
                topBar={ Column(Modifier.statusBarsPadding().padding(horizontal=16.dp,vertical=12.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        OutlinedIconButton({scope.launch{drawer.open()}},Modifier.size(46.dp).semantics{contentDescription="Menu"},shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Color(0xFF59437C))) { Mark("menu",tint=WhiteInk) }
                        Column(Modifier.weight(1f)) { val hour=LocalTime.now().hour; Text(when { hour<12 -> L("☀ Good morning","☀ Guten Morgen");hour<18 -> L("☀ Good afternoon","☀ Guten Tag");else -> L("☾ Good evening","☾ Guten Abend") },fontSize=11.sp,color=Lavender,fontWeight=FontWeight.SemiBold); Text(labels[route] ?: "WageTrack",fontSize=23.sp,fontWeight=FontWeight.SemiBold) }
                        if(route in listOf("dashboard","shifts","expenses","history","planning")) HeaderMonth(vm)
                    }

                } }, bottomBar={
                    Surface(Modifier.navigationBarsPadding().padding(horizontal=12.dp,vertical=10.dp),shape=RoundedCornerShape(30.dp),color=Purple,border=BorderStroke(1.dp,Color(0xFF59437C))) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                            val nav=listOf("dashboard" to L("Overview","Übersicht"),"shifts" to L("Hours","Zeiten"),"add" to "", "expenses" to L("Expenses","Ausgaben"),"history" to L("History","Verlauf"))
                            nav.forEach { (id,label) -> if(id=="add") Box(Modifier.weight(1f),contentAlignment=Alignment.Center) { Surface(onClick={add()},Modifier.size(52.dp).semantics{contentDescription="Add shift"},shape=RoundedCornerShape(18.dp),color=Lime,contentColor=Ink,border=BorderStroke(2.dp,Ink)){Box(contentAlignment=Alignment.Center){Mark("add",Modifier.size(28.dp))}} }
                            else Column(Modifier.weight(1f).heightIn(min=54.dp).clickable{navigate(id)}.semantics{selected=route==id}.testTag("nav-$id"),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) { Mark(when(id){"dashboard"->"home";"shifts"->"hours";else->id},Modifier.size(23.dp),if(route==id)Lime else Lavender); Spacer(Modifier.height(5.dp));Text(label,fontSize=10.sp,color=if(route==id)Lime else Lavender) } }
                        }
                    }
                }) { padding -> Box(Modifier.padding(padding).fillMaxSize().testTag("screen-$route")) {
                when(route) {
                    "dashboard" -> Overview(vm,::navigate,{add()},::edit)
                    "shifts" -> Hours(vm,::add,::edit){navigate("workplaces")}
                    "expenses" -> Expenses(vm)
                    "history" -> History(vm){navigate("workplaces")}
                    "planning" -> Planning(vm)
                    "settings","profile","pay","workplaces","appearance" -> Preferences(vm,route,::navigate)
                    "reminders","lock" -> DevicePage(vm,route)
                    "backup" -> BackupPage(vm)
                }
            } }
        }
        if(shiftOpen) ShiftEditor(vm,editShift,shiftDate,template,{shiftOpen=false})
    } }
}

@Composable private fun HeaderMonth(vm: MainViewModel) {
    val month by vm.selectedMonth.collectAsState()
    var open by remember { mutableStateOf(false) }
    Surface(onClick = { open = true }, color = Raised, contentColor = WhiteInk, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Lavender.copy(alpha=.4f))) {
        Text(java.time.YearMonth.parse(month).format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", Formatters.locale))+" ⌄", Modifier.padding(horizontal=12.dp,vertical=15.dp),fontSize=12.sp)
    }
    if(open) AlertDialog(onDismissRequest={open=false},title={Text(L("Select month","Monat auswählen"))},text={
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                TextButton(vm::prevMonth){Text("‹")};Text(Formatters.formatMonthTitle(month));TextButton(vm::nextMonth){Text("›")}
            }
            val ym=java.time.YearMonth.parse(month)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton({vm.setMonth(ym.minusYears(1).toString())}){Text("− 1 "+L("year","Jahr"))};TextButton({vm.setMonth(ym.plusYears(1).toString())}){Text("+ 1 "+L("year","Jahr"))}}
            TextButton({vm.setMonth(java.time.YearMonth.now().toString());open=false}){Text(L("This month","Dieser Monat"))}
        }
    },confirmButton={TextButton({open=false}){Text(L("Done","Fertig"))}})
}
