package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.paydaytracker.app.ui.common.Formatters
import kotlinx.coroutines.launch

private data class QuickInfo(val title: String, val value: Double, val note: String, val estimate: Boolean)

/** Account-wide monthly figures: personal expenses are not assigned to workplaces. */
@Composable fun QuickInfoCarousel(grossEarned: Double, estimatedNet: Double, expenses: Double, currency: String) {
    val cards = listOf(
        QuickInfo(L("Available after expenses", "Nach Ausgaben verfügbar"), estimatedNet-expenses,
            L("All workplaces · Includes planned shifts, minus monthly expenses", "Alle Arbeitsplätze · Mit geplanten Schichten, minus Monatsausgaben"), true),
        QuickInfo(L("Gross earned", "Brutto verdient"), grossEarned,
            L("All workplaces · Completed shifts, including estimated bonuses", "Alle Arbeitsplätze · Erledigte Schichten mit geschätzten Zuschlägen"), true),
        QuickInfo(L("Estimated net income", "Geschätztes Nettoeinkommen"), estimatedNet,
            L("All workplaces · Includes planned shifts, before expenses", "Alle Arbeitsplätze · Mit geplanten Schichten, vor Ausgaben"), true),
        QuickInfo(L("Monthly expenses", "Monatsausgaben"), expenses,
            L("All expenses recorded for the selected month", "Alle erfassten Ausgaben des ausgewählten Monats"), false)
    )
    val pager = rememberPagerState(pageCount={cards.size})
    val scope = rememberCoroutineScope()
    val position = L("Card", "Karte") + " ${pager.currentPage+1} / ${cards.size}"
    WageCard(color=Lime, modifier=Modifier.testTag("quick-info-card")) {
        HorizontalPager(state=pager, pageSpacing=20.dp,
            modifier=Modifier.fillMaxWidth().testTag("quick-info-pager").semantics { stateDescription=position }) { page ->
            val card = cards[page]
            Column(Modifier.fillMaxWidth().testTag("quick-info-$page"), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min=30.dp), verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(card.title.uppercase(), Modifier.weight(1f), fontSize=11.sp, lineHeight=15.sp, fontWeight=FontWeight.Bold)
                    if(card.estimate) Text(L("ESTIMATE", "GESCHÄTZT"), fontSize=8.sp,
                        modifier=Modifier.border(1.dp,Ink.copy(alpha=.5f),RoundedCornerShape(20.dp)).padding(5.dp))
                }
                val value=Formatters.formatMoney(card.value,currency)
                Text(value, fontSize=if(value.length>13)26.sp else 36.sp, fontWeight=FontWeight.Bold,
                    modifier=Modifier.testTag("quick-info-value-$page"))
                Text(card.note, fontSize=12.sp, lineHeight=17.sp, modifier=Modifier.heightIn(min=34.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
            Text(L("Swipe for more", "Wischen für mehr"), Modifier.weight(1f), fontSize=10.sp)
            cards.forEachIndexed { index, card ->
                IconButton(onClick={scope.launch { pager.animateScrollToPage(index) }},
                    modifier=Modifier.size(40.dp).testTag("quick-info-dot-$index").semantics {
                        contentDescription=card.title; selected=pager.currentPage==index
                    }) {
                    Box(Modifier.size(if(pager.currentPage==index)9.dp else 6.dp)
                        .background(Ink.copy(alpha=if(pager.currentPage==index)1f else .3f),CircleShape))
                }
            }
        }
    }
}
