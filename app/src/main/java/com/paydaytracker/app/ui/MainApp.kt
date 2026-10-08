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
    val profile by vm.profile.collectAsState(); val places by vm.workplaces.collectAsState(); val shifts by vm.shifts.collectAsState()
    val doc = remember(document) { JSONObject(document) }
    val context = LocalContext.current
    var legalAccepted by remember { mutableStateOf(com.paydaytracker.app.LegalAcceptance.isAccepted(context)) }
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
        if (!legalAccepted) {
            LegalWelcome(onLanguage = { lang -> vm.updateDocument { it.put("language", lang) } }, onAccepted = { legalAccepted = true })
            return@WageTrackTheme
        }
        if (onboard) { Onboarding(vm); return@WageTrackTheme }
        BackHandler(drawer.isOpen || route != "dashboard") { if(drawer.isOpen) scope.launch { drawer.close() } else back() }
        val labels = mapOf("dashboard" to (profile.name.trim().split(" ").firstOrNull()?.takeIf{it.isNotBlank()}?.let{L("Hey $it 👋","Hallo $it 👋")} ?: L("Your overview","Deine Übersicht")), "shifts" to L("Shift calendar","Schichtkalender"), "expenses" to L("Expenses","Ausgaben"), "history" to L("Monthly history","Monatsverlauf"), "settings" to L("Settings","Einstellungen"), "profile" to L("Profile","Profil"), "pay" to L("Pay & tax","Lohn & Steuern"), "planning" to L("Budgets & goals","Budgets & Sparziele"), "new-workplace" to L("Workplaces","Arbeitsplätze"), "workplaces" to L("Workplaces","Arbeitsplätze"), "reminders" to L("Reminders & widget","Erinnerungen & Widget"), "lock" to L("App lock","App-Sperre"), "backup" to L("Backup & restore","Sichern & wiederherstellen"), "appearance" to L("App appearance","App-Darstellung"))
        ModalNavigationDrawer(drawerState=drawer, drawerContent={ ModalDrawerSheet(drawerContainerColor=Purple,drawerContentColor=WhiteInk) {
            Column(Modifier.fillMaxHeight().widthIn(max=320.dp).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {Image(painterResource(R.drawable.app_icon),null,Modifier.size(40.dp));Text("WageTrack",Modifier.weight(1f),fontSize=24.sp,fontWeight=FontWeight.Bold);IconButton({scope.launch{drawer.close()}},Modifier.semantics{contentDescription="Close menu"}){Mark("close")}}
                HorizontalDivider(color=Lavender.copy(alpha=.3f));Spacer(Modifier.height(4.dp))
                listOf("dashboard","shifts","expenses","history","settings").forEach { id ->
                    val label=when(id){"dashboard"->L("Overview","Übersicht");"shifts"->L("Work hours","Arbeitszeiten");else->labels[id]!!}
                    Surface(onClick={navigate(id);scope.launch{drawer.close()}},shape=RoundedCornerShape(16.dp),color=if(route==id)Lime else Purple,contentColor=if(route==id)Ink else WhiteInk) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){Mark(when(id){"dashboard"->"home";"shifts"->"hours";else->id},Modifier.size(24.dp));Text(label,Modifier.weight(1f),fontSize=16.sp);Text("›",fontSize=24.sp)}
                    }
                }
                Spacer(Modifier.height(12.dp))
                val backup=JSONObject(AutoBackup.get(context).state())
                Surface(onClick={navigate("backup");scope.launch{drawer.close()}},shape=RoundedCornerShape(14.dp),color=Purple,contentColor=WhiteInk,border=BorderStroke(1.dp,Lavender.copy(alpha=.3f))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Mark("backup");Column{Text(if(backup.optString("status")=="saved")L("Backed up","Gesichert")else L("Set up backup","Sicherung einrichten"),fontSize=17.sp);Text(L("Manage backup","Sicherung verwalten"),fontSize=14.sp,color=Lavender)}}}
                Text("WageTrack ${com.paydaytracker.app.BuildConfig.VERSION_NAME}",fontSize=11.sp,color=Lavender)
            }
        } }) {
            Scaffold(containerColor=MaterialTheme.colorScheme.background, contentColor=WhiteInk,
                contentWindowInsets=WindowInsets.safeDrawing,
bottomBar={
                    Surface(Modifier.navigationBarsPadding().padding(horizontal=12.dp,vertical=10.dp),shape=RoundedCornerShape(30.dp),color=Purple,border=BorderStroke(1.dp,Color(0xFF59437C))) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                            val nav=listOf("dashboard" to L("Overview","Übersicht"),"shifts" to L("Hours","Zeiten"),"add" to "", "expenses" to L("Expenses","Ausgaben"),"history" to L("History","Verlauf"))
                            nav.forEach { (id,label) -> if(id=="add") Box(Modifier.weight(1f),contentAlignment=Alignment.Center) { Surface(onClick={add()},Modifier.size(52.dp).semantics{contentDescription="Add shift"},shape=RoundedCornerShape(18.dp),color=Lime,contentColor=Ink,border=BorderStroke(2.dp,Ink)){Box(contentAlignment=Alignment.Center){Mark("add",Modifier.size(28.dp))}} }
                            else Column(Modifier.weight(1f).heightIn(min=54.dp).clickable{navigate(id)}.semantics{selected=route==id}.testTag("nav-$id"),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) { Mark(when(id){"dashboard"->"home";"shifts"->"hours";else->id},Modifier.size(23.dp),if(route==id)Lime else Lavender); Spacer(Modifier.height(5.dp));Text(label,fontSize=10.sp,color=if(route==id)Lime else Lavender) } }
                        }
                    }
                }) { padding -> Box(Modifier.padding(padding).fillMaxSize().testTag("screen-$route")) {
                CompositionLocalProvider(LocalPageHeader provides {
                    Row(Modifier.fillMaxWidth().padding(top=16.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        OutlinedIconButton({scope.launch{drawer.open()}},Modifier.size(46.dp).semantics{contentDescription="Menu"},shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Color(0xFF59437C))){Mark("menu",tint=WhiteInk)}
                        Column(Modifier.weight(1f)){val hour=LocalTime.now().hour;Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){Mark(if(hour in 6..17)"sun"else "moon",Modifier.size(14.dp),Color(0xFFFFD400));Text(when{hour<12->L("Good morning","Guten Morgen");hour<18->L("Good afternoon","Guten Tag");else->L("Good evening","Guten Abend")},fontSize=11.sp,color=Lavender,fontWeight=FontWeight.SemiBold)};Text(labels[route] ?: "WageTrack",fontSize=23.sp,fontWeight=FontWeight.SemiBold)}
                        if(route in listOf("dashboard","shifts","expenses","history","planning"))HeaderMonth(vm)
                    }
                }) { key(route) { when(route) {
                    "dashboard" -> Overview(vm,::navigate,{add()},::edit)
                    "shifts" -> Hours(vm,::add,::edit,{navigate("workplaces")},{navigate("new-workplace")})
                    "expenses" -> Expenses(vm)
                    "history" -> History(vm){navigate("workplaces")}
                    "planning" -> Planning(vm)
                    "settings","profile","pay","workplaces","new-workplace","appearance" -> Preferences(vm,route,::navigate)
                    "reminders","lock" -> DevicePage(vm,route)
                    "backup" -> BackupPage(vm)
                    "privacy", "terms" -> LegalReader(route, ::back)
                } } }
            } }
        }
        if(shiftOpen) ShiftEditor(vm,editShift,shiftDate,template,{shiftOpen=false})
    } }
}

@Composable private fun HeaderMonth(vm: MainViewModel) {
    val month by vm.selectedMonth.collectAsState()
    var open by remember { mutableStateOf(false) }
    Surface(onClick = { open = true }, modifier=Modifier.semantics{contentDescription="Select month"}, color = Raised, contentColor = WhiteInk, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Lavender.copy(alpha=.4f))) {
        Text(java.time.YearMonth.parse(month).format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", Formatters.locale)), Modifier.padding(horizontal=12.dp,vertical=15.dp),fontSize=12.sp)
    }
    if(open) MonthChooser(month,{open=false}) {vm.setMonth(it);open=false}
}
