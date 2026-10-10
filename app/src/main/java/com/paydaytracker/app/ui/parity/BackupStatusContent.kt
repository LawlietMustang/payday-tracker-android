package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.paydaytracker.app.ui.common.Formatters
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONObject

@Composable fun BackupStatusContent(status: JSONObject) {
    val state = status.optString("displayStatus", "off")
    val onDark = LocalContentColor.current.luminance() > .5f
    val title = when(state) {
        "saved" -> L("Backed up", "Gesichert")
        "saving" -> L("Saving backup…", "Sicherung wird gespeichert…")
        "pending", "outdated" -> L("Changes not backed up", "Änderungen nicht gesichert")
        "error" -> L("Backup needs attention", "Sicherung prüfen")
        else -> L("Not backed up yet", "Noch nicht gesichert")
    }
    val tint = when(state) {
        "saved" -> if(onDark) Color(0xFF86E5AD) else Color(0xFF176B3A)
        "pending", "outdated", "error" -> if(onDark) Color(0xFFFFCF7D) else Color(0xFF805200)
        "saving" -> if(onDark) Color(0xFFAFCBFF) else Color(0xFF285CAA)
        else -> LocalContentColor.current.copy(alpha=.75f)
    }
    val icon = when(state) {
        "saved" -> Icons.Outlined.CheckCircle
        "saving", "pending" -> Icons.Outlined.CloudUpload
        "outdated", "error" -> Icons.Outlined.WarningAmber
        else -> Icons.Outlined.Info
    }
    Row(Modifier.fillMaxWidth().testTag("backup-status").semantics { stateDescription = title },
        verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(26.dp).testTag("backup-status-icon"), tint=tint)
        Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(title, color=tint, fontSize=15.sp, fontWeight=FontWeight.SemiBold)
            val savedAt = status.optLong("lastBackupAt")
            if(savedAt > 0) {
                val whenSaved = Instant.ofEpochMilli(savedAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Formatters.locale))
                Text(L("Last backup: ", "Letzte Sicherung: ") + whenSaved,
                    fontSize=11.sp, color=LocalContentColor.current.copy(alpha=.75f))
            }
        }
    }
}
