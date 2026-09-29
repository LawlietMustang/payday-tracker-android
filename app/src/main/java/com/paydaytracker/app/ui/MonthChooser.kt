package com.paydaytracker.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.NumberPicker
import com.paydaytracker.app.ui.parity.L
import com.paydaytracker.app.ui.common.Formatters
import java.time.YearMonth
import java.time.format.TextStyle

@Composable fun MonthChooser(value:String,dismiss:()->Unit,select:(String)->Unit) {
    val initial=YearMonth.parse(value)
    var month by remember{mutableIntStateOf(initial.monthValue)};var year by remember{mutableIntStateOf(initial.year)}
    val names=(1..12).map{java.time.Month.of(it).getDisplayName(TextStyle.SHORT,Formatters.locale)}.toTypedArray()
    AlertDialog(containerColor=androidx.compose.ui.graphics.Color.White,titleContentColor=com.paydaytracker.app.ui.parity.Ink,textContentColor=com.paydaytracker.app.ui.parity.Ink,onDismissRequest=dismiss,title={Text(L("Set month","Monat einstellen"))},text={Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
        AndroidView(factory={ctx->NumberPicker(ctx).apply{minValue=1;maxValue=12;displayedValues=names;this.value=month;wrapSelectorWheel=true;setOnValueChangedListener{_,_,n->month=n};contentDescription="Month"}},modifier=Modifier.weight(1f).height(180.dp))
        AndroidView(factory={ctx->NumberPicker(ctx).apply{minValue=1900;maxValue=2200;this.value=year;wrapSelectorWheel=false;setOnValueChangedListener{_,_,n->year=n};contentDescription="Year"}},modifier=Modifier.weight(1f).height(180.dp))
    }},confirmButton={TextButton({select(YearMonth.of(year,month).toString())}){Text(L("Set","Setzen"))}},dismissButton={Row{TextButton({select(YearMonth.now().toString())}){Text(L("Clear","Zurücksetzen"))};TextButton(dismiss){Text(L("Cancel","Abbrechen"))}}})
}
