package com.codka.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codka.app.R
import com.codka.app.data.PaymentEntity
import com.codka.app.parser.Currency
import com.codka.app.ui.AmountText
import com.codka.app.ui.components.HomeAppBar
import com.codka.app.ui.components.RepeatButton
import com.codka.app.ui.components.RepeatState
import com.codka.app.ui.components.Tag
import com.codka.app.ui.components.Wave
import com.codka.app.ui.theme.CodkaColors
import com.codka.app.ui.theme.CodkaShape
import com.codka.app.ui.theme.CodkaType

/** A payment stays "just arrived" on Home for 2 minutes, then Home goes back to listening. */
const val FRESH_MS = 2 * 60_000L

/**
 * Task 20, in the Codka design (boards 09 and 17): the payment card on top showing what the
 * voice just said, the round "Ku celi" button in the thumb zone. The bottom tabs are drawn
 * by the app around this screen.
 */
@Composable
fun HomeScreen(
    last: PaymentEntity?,
    now: Long,
    speaking: Boolean,
    ready: Boolean,
    onRepeat: (PaymentEntity) -> Unit,
    onStop: () -> Unit,
    onOpenHelper: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeAppBar(ready = ready, onOpenHelper = onOpenHelper)
        PaymentHero(last, fresh = last != null && now - last.receivedAt < FRESH_MS, speaking)

        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val state =
                when {
                    last == null -> RepeatState.Disabled
                    speaking -> RepeatState.Speaking
                    else -> RepeatState.Idle
                }
            RepeatButton(
                state = state,
                label = stringResource(
                    if (state ==
                        RepeatState.Speaking
                    ) {
                        R.string.speaking
                    } else {
                        R.string.say_again
                    }
                ),
                onClick = { if (speaking) onStop() else last?.let(onRepeat) }
            )
            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(
                    when (state) {
                        RepeatState.Speaking -> R.string.tap_to_stop
                        RepeatState.Idle -> R.string.tap_to_hear_again
                        RepeatState.Disabled -> R.string.no_money_yet
                    }
                ),
                style = CodkaType.Caption,
                color = CodkaColors.Muted
            )
        }
    }
}

/** The card at the top of Home (board 09): waiting, just received, or "may be fake". */
@Composable
private fun PaymentHero(last: PaymentEntity?, fresh: Boolean, speaking: Boolean) {
    val (bg, border) =
        when {
            last != null && fresh && last.isSuspicious ->
                CodkaColors.Alert50 to
                    CodkaColors.AlertLine
            last != null && fresh -> CodkaColors.Paid50 to CodkaColors.PaidLine
            else -> CodkaColors.White to CodkaColors.Line
        }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CodkaShape.Lg)
            .background(bg)
            .border(1.dp, border, CodkaShape.Lg)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            last == null || !fresh -> Waiting(last)
            last.isSuspicious -> Received(last, speaking, suspicious = true)
            else -> Received(last, speaking, suspicious = false)
        }
    }
}

@Composable
private fun Waiting(last: PaymentEntity?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Wave(CodkaColors.Indigo300, listOf(8, 14, 20, 12))
        Spacer(Modifier.size(12.dp))
        Text(
            stringResource(R.string.listening),
            style = CodkaType.Heading,
            color = CodkaColors.Indigo
        )
    }
    Text(
        stringResource(if (last == null) R.string.no_new_money else R.string.no_new_money_last),
        style = CodkaType.Body,
        color = CodkaColors.Muted
    )
    if (last != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                AmountText.format(last),
                style = CodkaType.AmountL.copy(fontSize = 28.sp, lineHeight = 32.sp),
                color = if (last.isSuspicious) CodkaColors.Alert700 else CodkaColors.Ink,
                textDecoration = if (last.isSuspicious) TextDecoration.LineThrough else null
            )
            Spacer(Modifier.size(10.dp))
            // Only the letter badge here, like the design: the full name would push the time off.
            Tag(CodkaColors.badge(last.provider), CodkaColors.of(last.provider), Color.White)
            Spacer(Modifier.size(10.dp))
            Text(
                AmountText.time(last.receivedAt),
                style = CodkaType.Caption,
                color = CodkaColors.Muted,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun Received(payment: PaymentEntity, speaking: Boolean, suspicious: Boolean) {
    val accent = if (suspicious) CodkaColors.Alert700 else CodkaColors.Paid700
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CodkaShape.Pill).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(if (suspicious) R.drawable.ic_warning else R.drawable.ic_check),
                null,
                Modifier.size(24.dp),
                tint = Color.White
            )
        }
        Spacer(Modifier.size(10.dp))
        Text(
            stringResource(if (suspicious) R.string.suspicious_title else R.string.money_arrived),
            style = CodkaType.Display.copy(fontSize = 24.sp, lineHeight = 28.sp),
            color = accent
        )
    }
    BigAmount(payment, suspicious)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Tag(
            AmountText.providerName(payment.provider),
            CodkaColors.of(payment.provider),
            Color.White,
            badge = CodkaColors.badge(payment.provider)
        )
        AmountText.lastDigits(payment.senderPhone)?.let {
            Spacer(Modifier.size(8.dp))
            Text(it, style = CodkaType.Button.copy(fontSize = 20.sp), color = CodkaColors.Ink)
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            listOfNotNull(
                AmountText.shortName(payment.senderName),
                AmountText.time(payment.receivedAt)
            )
                .joinToString(" · "),
            Modifier.weight(1f),
            style = CodkaType.Caption,
            color = CodkaColors.Muted
        )
        if (speaking) Wave(accent, listOf(12, 24, 28, 16, 22))
    }
}

/** The biggest thing on the screen: 64 sp, 52 sp when long; shillings put "SLSH" after, smaller. */
@Composable
private fun BigAmount(payment: PaymentEntity, suspicious: Boolean) {
    val usd = payment.currency == Currency.USD
    val number = (if (usd) "$" else "") + AmountText.number(payment)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            number,
            style = CodkaType.AmountXl.copy(fontSize = amountSize(number, usd)),
            color = if (suspicious) CodkaColors.Alert700 else CodkaColors.Ink,
            textDecoration = if (suspicious) TextDecoration.LineThrough else null,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Start
        )
        if (!usd) {
            Spacer(Modifier.size(8.dp))
            Text(
                "SLSH",
                Modifier.padding(bottom = 6.dp),
                style = CodkaType.AmountL.copy(fontSize = 28.sp),
                color = CodkaColors.Muted,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * The amount never wraps (design board 21): 64 sp, stepping down to 52 sp when long, counting
 * the "SLSH" after shillings, and to 44 sp for the very largest (millions of shillings).
 */
internal fun amountSize(number: String, usd: Boolean) = when (number.length + if (usd) 0 else 4) {
    in 0..7 -> 64.sp
    in 8..11 -> 52.sp
    else -> 44.sp
}
