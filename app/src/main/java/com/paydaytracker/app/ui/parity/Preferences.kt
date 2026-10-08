package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.paydaytracker.app.data.*
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters as F

@Composable fun Preferences(vm:MainViewModel,route:String,navigate:(String)->Unit) {
    val settings by vm.settings.collectAsState();val profile by vm.profile.collectAsState();val document by vm.document.collectAsState();val extraProfile=org.json.JSONObject(document).optJSONObject("profile") ?: org.json.JSONObject();val workplaces by vm.workplaces.collectAsState()
    var deletion by remember{mutableStateOf(false)};var workplaceEdit by remember{mutableStateOf<Workplace?>(null)};var wpOpen by remember{mutableStateOf(route=="new-workplace")};var wpDelete by remember{mutableStateOf<Workplace?>(null)}
    Page{when(route){
        "settings" -> {
            var appearanceOpen by remember{mutableStateOf(false)}
            Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface,contentColor=MaterialTheme.colorScheme.onSurface) {
                Column {
                    listOf("profile" to L("Profile","Profil"),"pay" to L("Pay & tax","Lohn & Steuern"),"workplaces" to L("Workplaces","Arbeitsplätze"),"planning" to L("Budgets & goals","Budgets & Sparziele"),"reminders" to L("Reminders & widget","Erinnerungen & Widget"),"lock" to L("App lock","App-Sperre"),"backup" to L("Backup & restore","Sichern & wiederherstellen"),"appearance" to L("App appearance","App-Darstellung"),"delete" to L("Delete data","Daten löschen")).forEach{(id,label)->
                        Row(Modifier.fillMaxWidth().clickable{when(id){"delete"->deletion=true;"appearance"->appearanceOpen=!appearanceOpen;else->navigate(id)}}.padding(horizontal=22.dp,vertical=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
                            val tint=if(id=="delete")Color(0xFFAA2447)else LocalContentColor.current
                            Mark(id,Modifier.size(24.dp),tint);Text(label,Modifier.weight(1f),color=tint,fontSize=16.sp,fontWeight=FontWeight.Medium);if(id!="appearance")Text("›",color=tint,fontSize=24.sp)
                        }
                        HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.5f))
                        if(id=="appearance"&&appearanceOpen)Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                            Choice(L("Language","Sprache"),LocalLanguage.current,listOf("en" to "English","de" to "Deutsch")){language->vm.updateDocument{it.put("language",language)}}
                            Choice(L("Theme","Darstellung"),settings.theme,listOf("system" to L("System","System"),"light" to L("Light cards","Helle Karten"),"dark" to L("Dark","Dunkel"))){vm.saveSettings(settings.copy(theme=it))}
                            CurrencyChoice(settings.currency){vm.saveSettings(settings.copy(currency=it))}
                        }
                    }
                    Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){GoogleAccountPanel()}
                }
            }
            WageCard {
                Heading(L("Legal", "Rechtliches"))
                LinkRow(L("Privacy policy", "Datenschutzerklärung")) { navigate("privacy") }
                LinkRow(L("Terms & conditions", "Nutzungsbedingungen")) { navigate("terms") }
            }
            Text("WageTrack ${com.paydaytracker.app.BuildConfig.VERSION_NAME}",fontSize=11.sp,color=Lavender)
        }
        "profile" -> {var name by remember(profile){mutableStateOf(profile.name)};var street by remember(profile){mutableStateOf(profile.street)};var city by remember(profile){mutableStateOf(profile.city)};var tax by remember(profile){mutableStateOf(profile.taxId)}
            var postcode by remember(document){mutableStateOf(extraProfile.optString("postcode"))}
            var country by remember(document){mutableStateOf(extraProfile.optString("country"))}
            var email by remember(document){mutableStateOf(extraProfile.optString("email"))}
            var phone by remember(document){mutableStateOf(extraProfile.optString("phone"))}
            WageCard{Heading(L("Personal information","Persönliche Angaben"));Field(L("Name","Name"),name,{name=it});Field(L("Street & number","Straße & Hausnummer"),street,{street=it});Field(L("Postcode","Postleitzahl"),postcode,{postcode=it});Field(L("City","Stadt"),city,{city=it});Field(L("Country","Land"),country,{country=it});Field(L("Email","E-Mail"),email,{email=it});Field(L("Phone","Telefon"),phone,{phone=it});Field(L("Tax ID (optional)","Steuer-ID (optional)"),tax,{tax=it});Action(L("Save profile","Profil speichern"),primary=true){vm.saveProfile(UserProfile(name=name.trim(),street=street.trim(),city=city.trim(),taxId=tax.trim()));vm.updateDocument{it.put("profile",(it.optJSONObject("profile") ?: org.json.JSONObject()).put("postcode",postcode.trim()).put("country",country.trim()).put("email",email.trim()).put("phone",phone.trim()))}}}
        }
        "appearance" -> WageCard{Choice(L("Language","Sprache"),LocalLanguage.current,listOf("en" to "English","de" to "Deutsch")){language->vm.updateDocument{it.put("language",language)}};Choice(L("Theme","Darstellung"),settings.theme,listOf("system" to L("System","System"),"light" to L("Light cards","Helle Karten"),"dark" to L("Dark","Dunkel"))){vm.saveSettings(settings.copy(theme=it))};CurrencyChoice(settings.currency){vm.saveSettings(settings.copy(currency=it))};Text(L("One currency for all workplaces. No automatic conversion.","Eine Währung für alle Arbeitsplätze. Keine automatische Umrechnung."),fontSize=12.sp)}
        "workplaces","new-workplace" -> {Action(L("+ New workplace","+ Neuer Arbeitsplatz"),primary=true){workplaceEdit=null;wpOpen=true};workplaces.forEach{w->WageCard{Heading(w.name);Text(F.formatMoney(w.wage,settings.currency)+"/h");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Action(L("Edit","Bearbeiten")){workplaceEdit=w;wpOpen=true};Action(L("Delete","Löschen"),enabled=workplaces.size>1){wpDelete=w}}}}}
        "pay" -> {var draft by remember(settings){mutableStateOf(settings)}
            var nightStart by remember(settings){mutableStateOf(settings.nightStart)}
            var nightEnd by remember(settings){mutableStateOf(settings.nightEnd)}
            val validNight=runCatching{java.time.LocalTime.parse(nightStart);java.time.LocalTime.parse(nightEnd)}.isSuccess
            WageCard{Heading(L("Pay settings","Lohneinstellungen"));NumberField(L("Gross hourly wage","Bruttostundenlohn"),draft.wage){draft=draft.copy(wage=it)};NumberField(L("Monthly hours target","Monatsstundenziel"),draft.target){draft=draft.copy(target=it)};NumberField(L("Savings target","Sparziel"),draft.savingsTarget){draft=draft.copy(savingsTarget=it)};CurrencyChoice(draft.currency){draft=draft.copy(currency=it)};Choice(L("Net pay estimate","Nettoschätzung"),draft.taxMode,listOf("germany" to L("Germany","Deutschland"),"manual" to L("Manual deduction","Manueller Abzug"))){draft=draft.copy(taxMode=it)}
                if(draft.taxMode=="manual")NumberField(L("Deduction (%)","Abzug (%)"),draft.deductionPercent){draft=draft.copy(deductionPercent=it.coerceIn(0.0,100.0))}else{Choice(L("Tax class","Steuerklasse"),draft.taxclass,listOf("I","II","III","IV","V","VI").map{it to it}){draft=draft.copy(taxclass=it)};Choice(L("Federal state","Bundesland"),draft.state,listOf("Baden-Württemberg","Bayern","Berlin","Brandenburg","Bremen","Hamburg","Hessen","Mecklenburg-Vorpommern","Niedersachsen","Nordrhein-Westfalen","Rheinland-Pfalz","Saarland","Sachsen","Sachsen-Anhalt","Schleswig-Holstein").map{it to it}){draft=draft.copy(state=it)};NumberField(L("Health additional contribution (%)","KV-Zusatzbeitrag (%)"),draft.health){draft=draft.copy(health=it)};Toggle(L("Church tax","Kirchensteuer"),checked=draft.church){draft=draft.copy(church=it)};Toggle(L("Childless","Kinderlos"),checked=draft.childless){draft=draft.copy(childless=it)}}
            }
            WageCard{Heading(L("Bonuses","Zuschläge"));NumberField(L("Overtime after (hours)","Überstunden ab (Stunden)"),draft.overtimeAfter){draft=draft.copy(overtimeAfter=it)};NumberField(L("Overtime (%)","Überstunden (%)"),draft.overtimeRate){draft=draft.copy(overtimeRate=it)};Field(L("Night starts","Nachtbeginn"),nightStart,{nightStart=it});Field(L("Night ends","Nachtende"),nightEnd,{nightEnd=it});NumberField(L("Night (%)","Nacht (%)"),draft.nightRate){draft=draft.copy(nightRate=it)};NumberField(L("Sunday (%)","Sonntag (%)"),draft.sundayRate){draft=draft.copy(sundayRate=it)};NumberField(L("Holiday (%)","Feiertag (%)"),draft.holidayRate){draft=draft.copy(holidayRate=it)};Text(L("Usual working days","Übliche Arbeitstage"))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) { (1..7).forEach { n -> val day=n%7
                    FilterChip(selected=day in draft.days,onClick={draft=draft.copy(days=if(day in draft.days)draft.days-day else draft.days+day)},label={Text(java.time.DayOfWeek.of(n).getDisplayName(java.time.format.TextStyle.NARROW,F.locale),fontSize=10.sp)})
                }}
                Toggle(L("Complete past planned shifts automatically","Vergangene geplante Schichten automatisch abschließen"),checked=draft.autoCompletePlanned){draft=draft.copy(autoCompletePlanned=it)};Action(L("Save settings","Einstellungen speichern"),primary=true,enabled=validNight){vm.saveSettings(draft.copy(nightStart=nightStart,nightEnd=nightEnd))}}
        }
    }}
    if(deletion)AlertDialog(onDismissRequest={deletion=false},title={Text(L("Delete all app data?","Alle App-Daten löschen?"))},text={Text(L("This removes records on this device. Export a backup first if you want to keep them.","Dies entfernt die Daten auf diesem Gerät. Exportiere vorher eine Sicherung, um sie zu behalten."))},confirmButton={TextButton({vm.resetData();deletion=false;navigate("dashboard")}){Text(L("Delete all data","Alle Daten löschen"))}},dismissButton={TextButton({deletion=false}){Text(L("Cancel","Abbrechen"))}})
    if(wpOpen){var name by remember{mutableStateOf(workplaceEdit?.name ?: "")};var wage by remember{mutableStateOf(workplaceEdit?.wage?.toString() ?: settings.wage.toString())};val n=wage.replace(',','.').toDoubleOrNull();AlertDialog(onDismissRequest={wpOpen=false},title={Text(L("Workplace","Arbeitsplatz"))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Field(L("Name","Name"),name,{name=it});Field(L("Hourly wage","Stundenlohn"),wage,{wage=it})}},confirmButton={TextButton({if(workplaceEdit==null)vm.addWorkplace(name.trim(),n!!)else vm.updateWorkplace(workplaceEdit!!.copy(name=name.trim(),wage=n!!));wpOpen=false},enabled=name.isNotBlank()&&n!=null&&n.isFinite()&&n>0){Text(L("Save","Speichern"))}},dismissButton={TextButton({wpOpen=false}){Text(L("Cancel","Abbrechen"))}})}
    wpDelete?.let{w->AlertDialog(onDismissRequest={wpDelete=null},title={Text(L("Delete workplace?","Arbeitsplatz löschen?"))},text={Text(L("Shifts will move to the remaining workplace. Payslips for this workplace will be removed.","Schichten werden einem verbleibenden Arbeitsplatz zugeordnet. Die Abrechnungen dieses Arbeitsplatzes werden entfernt."))},confirmButton={TextButton({vm.deleteWorkplace(w.id);wpDelete=null}){Text(L("Delete","Löschen"))}},dismissButton={TextButton({wpDelete=null}){Text(L("Cancel","Abbrechen"))}})}
}
@Composable fun NumberField(label:String,value:Double,change:(Double)->Unit){var input by remember{mutableStateOf(value.toString())};LaunchedEffect(value){if(input.replace(',','.').toDoubleOrNull()!=value)input=value.toString()};Field(label,input,{input=it;it.replace(',','.').toDoubleOrNull()?.takeIf{n->n.isFinite()&&n>=0}?.let(change)})}
@Composable fun CurrencyChoice(value:String,change:(String)->Unit){var search by remember{mutableStateOf("")};var open by remember{mutableStateOf(false)};LinkRow(L("Currency","Währung"),value){open=true};if(open)AlertDialog(onDismissRequest={open=false},title={Text(L("Currency","Währung"))},text={Column{Field(L("Search by name or code","Nach Name oder Code suchen"),search,{search=it});androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=340.dp)){val currencies=java.util.Currency.getAvailableCurrencies().sortedBy{it.currencyCode}.filter{it.currencyCode.contains(search,true)||it.getDisplayName(F.locale).contains(search,true)};items(currencies.size){i->val c=currencies[i];TextButton({change(c.currencyCode);open=false}){Text(c.currencyCode+" · "+c.getDisplayName(F.locale))}}}}},confirmButton={TextButton({open=false}){Text(L("Close","Schließen"))}})}
