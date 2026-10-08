package com.paydaytracker.app.ui.parity

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paydaytracker.app.LegalAcceptance
import com.paydaytracker.app.R

private fun readDocument(context: Context, kind: String, language: String): String? = runCatching {
    require(kind in listOf("privacy", "terms"))
    context.assets.open("legal/$kind-${if (language == "en") "en" else "de"}.md")
        .bufferedReader().use { it.readText() }.also { require(it.isNotBlank()) }
}.getOrNull()

@Composable
fun LegalWelcome(onLanguage: (String) -> Unit, onAccepted: () -> Unit) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    var reading by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val available = remember(language) {
        readDocument(context, "terms", language) != null && readDocument(context, "privacy", language) != null
    }
    BackHandler { if (reading != null) reading = null else (context as? Activity)?.finish() }
    Surface(Modifier.fillMaxSize(), color = Ink, contentColor = WhiteInk) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().testTag("legal-welcome")) {
            if (reading != null) {
                key(reading, language) { LegalReader(reading!!) { reading = null } }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.app_icon), null, Modifier.size(42.dp))
                        Text("WageTrack", Modifier.weight(1f).padding(start = 12.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        TextButton({ onLanguage(if (language == "en") "de" else "en") }, Modifier.testTag("legal-language")) {
                            Text(if (language == "en") "Deutsch" else "English", color = Lime)
                        }
                    }
                    Heading(L("Welcome to WageTrack", "Willkommen bei WageTrack"))
                    Text(L("Your hours. Your earnings. Your control.", "Deine Zeit. Dein Verdienst. Deine Kontrolle."), color = Lavender)
                    WageCard {
                        Text(L("Before you start", "Bevor du startest"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(L("Read how the app works and how your information is handled. Both documents are always available in Settings.", "Lies, wie die App funktioniert und mit deinen Daten umgeht. Beide Dokumente findest du jederzeit in den Einstellungen."), fontSize = 14.sp)
                        Action(L("Privacy policy", "Datenschutzerklärung"), Modifier.fillMaxWidth().testTag("open-privacy")) { reading = "privacy" }
                        Action(L("Terms & conditions", "Nutzungsbedingungen"), Modifier.fillMaxWidth().testTag("open-terms")) { reading = "terms" }
                    }
                    Text(L("No account required. Notifications and Google sign-in remain optional.", "Kein Konto erforderlich. Benachrichtigungen und Google-Anmeldung bleiben freiwillig."), color = Lavender, fontSize = 13.sp)
                    Text(L("Draft preview · Publisher details pending", "Entwurf · Anbieterangaben fehlen noch"), color = Lavender, fontSize = 12.sp)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(L("By tapping Accept & continue, you agree to the Terms & conditions and acknowledge the Privacy policy. This does not give consent to optional data processing.", "Mit Akzeptieren & weiter stimmst du den Nutzungsbedingungen zu und bestätigst die Kenntnisnahme der Datenschutzerklärung. Dies ist keine Einwilligung in optionale Datenverarbeitung."), fontSize = 12.sp, color = Lavender)
                    if (!available || failed) Text(L("Could not load or save the agreement. Please try again.", "Die Vereinbarung konnte nicht geladen oder gespeichert werden. Bitte erneut versuchen."), color = Pink)
                    Action(L("Accept & continue", "Akzeptieren & weiter"), Modifier.fillMaxWidth().testTag("accept-legal"), primary = true, enabled = available && !saving) {
                        saving = true
                        failed = !LegalAcceptance.accept(context, language)
                        saving = false
                        if (!failed) onAccepted()
                    }
                    TextButton({ (context as? Activity)?.finish() }, Modifier.align(Alignment.CenterHorizontally).testTag("decline-legal")) {
                        Text(L("Not now · Close app", "Jetzt nicht · App schließen"), color = Lavender)
                    }
                }
            }
        }
    }
}

/** Packaged text rendered with native Compose; reading never requires a network request. */
@Composable
fun LegalReader(kind: String, back: () -> Unit) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val uriHandler = LocalUriHandler.current
    val document = remember(kind, language) { readDocument(context, kind, language) }
    val title = if (kind == "privacy") L("Privacy policy", "Datenschutzerklärung") else L("Terms & conditions", "Nutzungsbedingungen")
    var linkFailed by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().testTag("legal-$kind")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(back, Modifier.testTag("legal-back")) { Text(L("Back", "Zurück"), color = Lime) }
            Text(title, Modifier.weight(1f), color = WhiteInk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
        HorizontalDivider(color = Lavender.copy(alpha = .25f))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (document == null) Text(L("Document unavailable. Please reopen this page.", "Dokument nicht verfügbar. Bitte öffne diese Seite erneut."), color = WhiteInk)
            else WageCard {
                document.trim().split(Regex("\\n\\s*\\n")).forEach { paragraph ->
                    when {
                        paragraph.startsWith("https://") && !paragraph.contains('\n') -> TextButton({
                            linkFailed = runCatching { uriHandler.openUri(paragraph.trim()) }.isFailure
                        }) { Text(paragraph, fontSize = 13.sp) }
                        else -> SelectionContainer {
                            Text(paragraph.removePrefix("## ").removePrefix("# "),
                                fontSize = if (paragraph.startsWith("#")) 19.sp else 14.sp,
                                lineHeight = if (paragraph.startsWith("#")) 26.sp else 22.sp,
                                fontWeight = if (paragraph.startsWith("#")) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
                if (linkFailed) Text(L("No browser could open this link.", "Der Link konnte nicht im Browser geöffnet werden."))
            }
        }
    }
}
