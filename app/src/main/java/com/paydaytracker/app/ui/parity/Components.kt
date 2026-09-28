package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.paydaytracker.app.R
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters
import java.time.YearMonth

val Ink = Color(0xFF180E33)
val Purple = Color(0xFF241452)
val Raised = Color(0xFF2E1A63)
val Lime = Color(0xFFCFFF3D)
val Pink = Color(0xFFFF4F8B)
val Lavender = Color(0xFFB7A6DE)
val WhiteInk = Color(0xFFF3ECFF)
val LocalLanguage = compositionLocalOf { "en" }
@Composable fun L(en: String, de: String) = if (LocalLanguage.current == "en") en else de

@Composable fun WageCard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth().drawBehind { drawRoundRect(Color(0xFF0E082A), topLeft = Offset(3.dp.toPx(), 5.dp.toPx()), cornerRadius = CornerRadius(24.dp.toPx())) },
        shape = RoundedCornerShape(24.dp), color = color, contentColor = if (color == Lime) Ink else MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}
@Composable fun Action(label: String, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true, compact: Boolean = false, onClick: () -> Unit) {
    Button(onClick, modifier.heightIn(min = 44.dp), enabled = enabled, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (primary) Ink else MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) Lime else MaterialTheme.colorScheme.surface, contentColor = if (primary) Ink else MaterialTheme.colorScheme.onSurface), contentPadding = PaddingValues(horizontal = if(compact)4.dp else 16.dp, vertical = 10.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = if(compact)12.sp else 14.sp, maxLines = if(compact)1 else Int.MAX_VALUE)
    }
}
@Composable fun Eyebrow(text: String) { Text(text.uppercase(), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = LocalContentColor.current.copy(alpha = .7f)) }
@Composable fun Heading(text: String) { Text(text, fontSize = 23.sp, fontWeight = FontWeight.SemiBold) }
@Composable fun Stat(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, Modifier.weight(1f), fontSize = 13.sp, color = LocalContentColor.current.copy(alpha = .7f)); Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
} }
@Composable fun Choice(label: String, value: String, choices: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val tint = LocalContentColor.current
    Column { if (label.isNotEmpty()) Text(label, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        Box { OutlinedButton({ open = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = tint)) {
            Text(choices.firstOrNull { it.first == value }?.second ?: value, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text("⌄")
        }; DropdownMenu(open, { open = false }) { choices.forEach { (key, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(key); open = false }) } } }
    }
}
@Composable fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(12.dp))
}
@Composable fun Toggle(title: String, subtitle: String = "", checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = LocalContentColor.current.copy(alpha = .65f)) }
        Switch(checked, change, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF5831BD), checkedThumbColor = Color.White))
    }
}
@Composable fun LinkRow(title: String, subtitle: String = "", icon: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Mark(icon, Modifier.size(24.dp))
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = LocalContentColor.current.copy(alpha = .65f)) }
        Text("›", fontSize = 24.sp)
    }
}
@Composable fun Mark(kind: String, modifier: Modifier = Modifier.size(24.dp), tint: Color = LocalContentColor.current) {
    val res = when (kind) { "sun" -> R.drawable.wt_sun; "moon" -> R.drawable.wt_moon; "profile" -> R.drawable.wt_profile; "workplaces" -> R.drawable.wt_workplaces; "planning" -> R.drawable.wt_budgets_goals; "reminders" -> R.drawable.wt_reminders_widget; "lock" -> R.drawable.wt_lock; "backup" -> R.drawable.wt_cloud_sync; "info" -> R.drawable.wt_info; else -> null }
    if (res != null) { Icon(painterResource(res), null, modifier, tint); return }
    Canvas(modifier) {
        val s = size.minDimension / 24f
        fun line(a: Float, b: Float, c: Float, d: Float) = drawLine(tint, Offset(a*s,b*s), Offset(c*s,d*s), 1.8f*s, StrokeCap.Round)
        when(kind) {
            "home" -> { val p = Path().apply { moveTo(3*s,10*s); lineTo(12*s,3*s); lineTo(21*s,10*s); lineTo(21*s,22*s); lineTo(3*s,22*s); close() }; drawPath(p,tint, style=Stroke(1.8f*s)) }
            "hours" -> { drawCircle(tint,10*s, Offset(12*s,12*s), style=Stroke(1.8f*s)); line(12f,6f,12f,12f); line(12f,12f,16f,15f) }
            "expenses" -> { drawArc(tint,60f,240f,false,Offset(7*s,2*s),Size(14*s,20*s), style=Stroke(1.8f*s,cap=StrokeCap.Round)); line(3f,9f,17f,9f); line(3f,15f,17f,15f) }
            "history" -> { drawRect(tint,Offset(4*s,2*s),Size(16*s,20*s), style=Stroke(1.8f*s)); line(8f,7f,16f,7f); line(8f,12f,16f,12f); line(8f,17f,13f,17f) }
            "add" -> { line(12f,3f,12f,21f); line(3f,12f,21f,12f) }
            "menu" -> { line(3f,5f,21f,5f); line(3f,12f,21f,12f); line(3f,19f,21f,19f) }
            else -> { drawCircle(tint,9*s,style=Stroke(1.8f*s)); line(12f,8f,12f,16f) }
        }
    }
}
@Composable fun WorkplacePicker(vm: MainViewModel, manage: () -> Unit) {
    val filter by vm.workplaceFilter.collectAsState(); val places by vm.workplaces.collectAsState()
    Surface(color = Purple, contentColor = WhiteInk, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF493666))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Choice("", filter, listOf("all" to L("All workplaces", "Alle Arbeitsplätze")) + places.map { it.id to it.name }, vm::setFilter) }
            IconButton(manage) { Mark("workplaces") }
        }
    }
}
