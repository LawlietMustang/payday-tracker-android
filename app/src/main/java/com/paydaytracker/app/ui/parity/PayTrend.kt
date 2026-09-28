package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paydaytracker.app.data.*
import com.paydaytracker.app.ui.common.Formatters as F
import java.time.YearMonth

/** Missing months are gaps. Each comparison uses only workplaces with a payslip. */
@Composable fun PayTrend(payslips:List<Payslip>,shifts:List<Shift>,settings:AppSettings,places:List<Workplace>,filter:String){
    val rows=payslips.filter{filter=="all"||it.workplaceId==filter}
    val latest=rows.maxOfOrNull{it.month} ?: return
    val months=(11 downTo 0).map{YearMonth.parse(latest).minusMonths(it.toLong()).toString()}
    val points=months.map{m->val entries=rows.filter{it.month==m};if(entries.isEmpty())null else entries.sumOf{PayrollCalculator.summary(m,shifts,settings,places,it.workplaceId).est.net} to entries.sumOf{it.actualNet}}
    val max=points.filterNotNull().flatMap{listOf(it.first,it.second)}.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val title=L("Net pay comparison · 12 months","Netto im Vergleich · 12 Monate")
    WageCard{Heading(title);Canvas(Modifier.fillMaxWidth().height(135.dp).semantics{contentDescription=title}){
        val bottom=size.height-8.dp.toPx();val top=8.dp.toPx();val left=6.dp.toPx();val width=size.width-12.dp.toPx()
        fun point(i:Int,v:Double)=Offset(left+width*i/11f,bottom-(v/max).toFloat()*(bottom-top))
        repeat(3){i->val y=top+(bottom-top)*i/2;drawLine(Color.Gray.copy(alpha=.2f),Offset(left,y),Offset(left+width,y),1.dp.toPx())}
        for(actual in listOf(false,true)){val color=if(actual)Pink else Color(0xFF8058CB);for(i in points.indices){val p=points[i] ?: continue;val pos=point(i,if(actual)p.second else p.first);drawCircle(color,3.dp.toPx(),pos);if(i>0)points[i-1]?.let{prev->drawLine(color,point(i-1,if(actual)prev.second else prev.first),pos,2.dp.toPx(),pathEffect=if(actual)null else PathEffect.dashPathEffect(floatArrayOf(7f,5f)))}}}
    };Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(months.first(),fontSize=10.sp);Text(months.last(),fontSize=10.sp)};Text(L("Purple dashed: estimated · Pink: payslip","Lila gestrichelt: geschätzt · Pink: Abrechnung"),fontSize=11.sp);points.forEachIndexed{i,p->if(p!=null)Stat(months[i],F.formatMoney(p.first,settings.currency)+" / "+F.formatMoney(p.second,settings.currency))}}
}
