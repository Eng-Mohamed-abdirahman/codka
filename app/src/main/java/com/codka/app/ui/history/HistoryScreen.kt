package com.codka.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.codka.app.R
import com.codka.app.data.PaymentEntity
import com.codka.app.data.PaymentStore
import com.codka.app.ui.AmountText
import com.codka.app.ui.components.Badge
import com.codka.app.ui.components.BigIcon
import com.codka.app.ui.components.PlayButton
import com.codka.app.ui.theme.CodkaColors
import com.codka.app.ui.theme.CodkaShape
import com.codka.app.ui.theme.CodkaSize
import com.codka.app.ui.theme.CodkaType
import java.util.TimeZone

enum class Period { Today, Yesterday, All }

/**
 * Task 21, in the Codka design (boards 10 and 18): Maanta / Shalay / Dhammaan, newest first.
 * Tap any row to hear it again. A payment that may be fake is red, struck through and
 * labelled "Been abuur" so nobody counts it as money.
 */
@Composable
fun HistoryScreen(
    today: List<PaymentEntity>,
    yesterday: List<PaymentEntity>,
    all: List<PaymentEntity>,
    now: Long,
    onPlay: (PaymentEntity) -> Unit
) {
    // Opens on "Maanta" every time (the design resets it on each visit).
    var period by remember { mutableStateOf(Period.Today) }
    val shown =
        when (period) {
            Period.Today -> today
            Period.Yesterday -> yesterday
            Period.All -> all
        }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.tab_history),
            Modifier.padding(top = 8.dp),
            style = CodkaType.Title,
            color = CodkaColors.Indigo
        )
        PeriodTabs(period) { period = it }

        if (shown.isEmpty()) {
            Empty(period, Modifier.weight(1f))
        } else {
            val groups = groupByDay(shown, now)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                groups.forEach { (label, payments) ->
                    item(key = "day-$label") { DayHeader(label, payments.size) }
                    items(payments, key = { it.id }) { HistoryRow(it, onPlay) }
                }
            }
        }
    }
}

/** Splits a list into days, labelled Maanta / Shalay / "14/11". */
@Composable
private fun groupByDay(
    payments: List<PaymentEntity>,
    now: Long
): List<Pair<String, List<PaymentEntity>>> {
    val zone = TimeZone.getDefault()
    val todayStart = PaymentStore.dayOf(now, zone).first
    val yesterdayStart = PaymentStore.dayOf(todayStart - 1, zone).first
    val todayLabel = stringResource(R.string.period_today)
    val yesterdayLabel = stringResource(R.string.period_yesterday)
    return payments
        .groupBy { PaymentStore.dayOf(it.receivedAt, zone).first }
        .map { (dayStart, list) ->
            val label =
                when (dayStart) {
                    todayStart -> todayLabel
                    yesterdayStart -> yesterdayLabel
                    else -> AmountText.day(dayStart, zone)
                }
            label to list
        }
}

@Composable
private fun PeriodTabs(selected: Period, onSelect: (Period) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CodkaShape.Pill).background(CodkaColors.Sand).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            Period.Today to R.string.period_today,
            Period.Yesterday to R.string.period_yesterday,
            Period.All to R.string.period_all
        ).forEach { (period, label) ->
            val on = period == selected
            Text(
                stringResource(label),
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CodkaShape.Pill)
                    .background(if (on) CodkaColors.Indigo else CodkaColors.Sand)
                    .clickable { onSelect(period) }
                    .semantics {
                        role = Role.Tab
                        this.selected = on
                    }.padding(top = 12.dp),
                style = CodkaType.Button.copy(fontSize = CodkaType.Body.fontSize),
                color = if (on) CodkaColors.Cream else CodkaColors.Muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayHeader(label: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(label, style = CodkaType.Heading, color = CodkaColors.Indigo)
        Text(
            stringResource(R.string.count_payments, count),
            style = CodkaType.Caption,
            color = CodkaColors.Muted
        )
    }
}

/** One payment (board 10): badge, amount, last digits and time, play. The whole row plays it. */
@Composable
private fun HistoryRow(payment: PaymentEntity, onPlay: (PaymentEntity) -> Unit) {
    val fake = payment.isSuspicious
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = CodkaSize.Row)
            .clip(CodkaShape.Md)
            .background(if (fake) CodkaColors.Alert50 else CodkaColors.White)
            .border(1.dp, if (fake) CodkaColors.AlertLine else CodkaColors.Line, CodkaShape.Md)
            .clickable { onPlay(payment) }
            .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (fake) {
            Badge(CodkaColors.Alert700, icon = R.drawable.ic_warning)
        } else {
            Badge(CodkaColors.of(payment.provider), text = CodkaColors.badge(payment.provider))
        }
        Column(Modifier.weight(1f)) {
            Text(
                AmountText.format(payment),
                style = CodkaType.Button.copy(fontSize = CodkaType.Heading.fontSize * 1.1f),
                color = if (fake) CodkaColors.Alert700 else CodkaColors.Ink,
                textDecoration = if (fake) TextDecoration.LineThrough else null,
                maxLines = 1
            )
            val time = AmountText.time(payment.receivedAt)
            Text(
                if (fake) {
                    stringResource(R.string.fake_label) + " · " + time
                } else {
                    listOfNotNull(
                        AmountText.lastDigits(payment.senderPhone),
                        time
                    ).joinToString(" · ")
                },
                style = CodkaType.Caption,
                color = if (fake) CodkaColors.Alert700 else CodkaColors.Muted,
                fontWeight = if (fake) FontWeight.Bold else null
            )
        }
        PlayButton(
            onClick = { onPlay(payment) },
            label = stringResource(if (fake) R.string.play_warning else R.string.play)
        )
    }
}

@Composable
private fun Empty(period: Period, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BigIcon(R.drawable.ic_history)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(
                when (period) {
                    Period.Today -> R.string.empty_today
                    Period.Yesterday -> R.string.empty_yesterday
                    Period.All -> R.string.empty_all
                }
            ),
            style = CodkaType.Heading.copy(fontSize = CodkaType.Title.fontSize * 0.92f),
            color = CodkaColors.Indigo,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_hint),
            Modifier.widthIn(max = 280.dp),
            style = CodkaType.Body,
            color = CodkaColors.Muted,
            textAlign = TextAlign.Center
        )
    }
}
