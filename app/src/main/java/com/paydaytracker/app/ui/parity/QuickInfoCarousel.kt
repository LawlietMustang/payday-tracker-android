package com.paydaytracker.app.ui.parity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
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
            L("Net incl. planned shifts minus expenses", "Netto inkl. geplanter Schichten minus Ausgaben"), true),
        QuickInfo(L("Gross earned", "Brutto verdient"), grossEarned,
            L("Completed shifts incl. estimated bonuses", "Erledigte Schichten inkl. geschätzter Zuschläge"), true),
        QuickInfo(L("Estimated net income", "Geschätztes Nettoeinkommen"), estimatedNet,
            L("Includes planned shifts, before expenses", "Inkl. geplanter Schichten, vor Ausgaben"), true),
        QuickInfo(L("Monthly expenses", "Monatsausgaben"), expenses,
            L("Expenses recorded for the selected month", "Erfasste Ausgaben des ausgewählten Monats"), false)
    )
    // Start in the middle of repeating pages: both swipe directions wrap naturally,
    // including the animation from the last card to the first. Never animate back
    // through every card or jump/reset the pager while a finger is dragging it.
    val firstPage = (Int.MAX_VALUE / 2 / cards.size) * cards.size
    val pager = rememberPagerState(initialPage=firstPage, pageCount={Int.MAX_VALUE})
    val scope = rememberCoroutineScope()
    val current = pager.currentPage % cards.size
    val nextLabel = L("Next card", "Nächste Karte")
    val previousLabel = L("Previous card", "Vorherige Karte")
    val position = L("Card", "Karte") + " ${current+1} / ${cards.size}"
    fun advance(direction: Int) { scope.launch { pager.animateScrollToPage(pager.targetPage+direction) } }
    Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(L("Quick view", "Schnellübersicht"), fontSize=22.sp, fontWeight=FontWeight.Bold,
            modifier=Modifier.testTag("quick-info-heading"))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .testTag("quick-info-card")
            .clickable(onClickLabel=nextLabel) { advance(1) }
            .semantics {
                stateDescription=position
                customActions=listOf(CustomAccessibilityAction(previousLabel) { advance(-1); true })
            }) {
            HorizontalPager(state=pager, pageSpacing=12.dp,
                modifier=Modifier.fillMaxWidth().testTag("quick-info-pager")) { virtualPage ->
                val page = virtualPage % cards.size
                val card = cards[page]
                WageCard(color=Lime, padding=24.dp, modifier=Modifier.testTag("quick-info-$page")) {
                    Row(Modifier.fillMaxWidth().heightIn(min=24.dp), verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(card.title.uppercase(), Modifier.weight(1f), fontSize=11.sp, lineHeight=15.sp, fontWeight=FontWeight.Bold)
                        if(card.estimate) Text(L("ESTIMATE", "GESCHÄTZT"), fontSize=9.sp, fontWeight=FontWeight.Bold,
                            modifier=Modifier.border(1.dp,Ink,RoundedCornerShape(20.dp))
                                .padding(horizontal=10.dp,vertical=4.dp))
                    }
                    val value=Formatters.formatMoney(card.value,currency)
                    Text(value, fontSize=if(value.length>13)26.sp else 36.sp, fontWeight=FontWeight.Bold,
                        modifier=Modifier.testTag("quick-info-value-$page"))
                    Text(card.note, fontSize=12.sp, lineHeight=17.sp, modifier=Modifier.heightIn(min=34.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().testTag("quick-info-indicators"),
            horizontalArrangement=Arrangement.Center, verticalAlignment=Alignment.CenterVertically) {
            cards.forEachIndexed { index, card ->
                val active=current==index
                IconButton(onClick={scope.launch {
                    val target=pager.targetPage
                    val forward=(index-target%cards.size+cards.size)%cards.size
                    val delta=if(forward>cards.size/2)forward-cards.size else forward
                    pager.animateScrollToPage(target+delta)
                }}, modifier=Modifier.size(40.dp).testTag("quick-info-dot-$index").semantics {
                    contentDescription=card.title; selected=active
                }) {
                    Box(Modifier.width(if(active)22.dp else 7.dp).height(7.dp)
                        .background(if(active)Lime else Lavender.copy(alpha=.45f),RoundedCornerShape(50)))
                }
            }
        }
    }
}
