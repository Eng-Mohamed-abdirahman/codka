package com.codka.app.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codka.app.R
import com.codka.app.data.PaymentEntity
import com.codka.app.parser.Currency
import com.codka.app.ui.AmountText
import com.codka.app.ui.theme.CodkaColors

/**
 * Task 20: the seller's screen. The last payment in big digits on its provider's colour
 * (red if it may be fake), one huge button to hear it again, and today's total.
 * Built for people who may not read: the colour and the speaker button carry the meaning.
 */
@Composable
fun HomeScreen(
    last: PaymentEntity?,
    today: List<PaymentEntity>,
    onSayAgain: (PaymentEntity) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSetup: () -> Unit,
    smsAllowed: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Small, out of the way: only the helper who sets the phone up needs it.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onOpenSetup) {
                Icon(painterResource(R.drawable.ic_more), stringResource(R.string.open_helper))
            }
        }
        // Without SMS permission Codka hears nothing: say so loudly, in red.
        if (!smsAllowed) {
            Button(
                onClick = onOpenSetup,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors =
                ButtonDefaults.buttonColors(
                    containerColor = CodkaColors.Suspicious,
                    contentColor = CodkaColors.OnSuspicious
                )
            ) {
                Icon(painterResource(R.drawable.ic_warning), null, Modifier.size(28.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.sms_not_allowed), fontSize = 18.sp)
            }
        }

        if (last == null) {
            NoPaymentYet(Modifier.weight(1f))
        } else {
            LastPaymentCard(last)
            Spacer(Modifier.weight(1f))
            SayAgainButton(onClick = { onSayAgain(last) })
            Spacer(Modifier.weight(1f))
        }

        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(painterResource(R.drawable.ic_list), null, Modifier.size(32.dp))
            Spacer(Modifier.size(12.dp))
            Text(
                AmountText.totals(today)?.let { stringResource(R.string.today_total, it) }
                    ?: stringResource(R.string.today_none),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LastPaymentCard(payment: PaymentEntity) {
    val background = CodkaColors.cardOf(payment)
    val ink = CodkaColors.onCardOf(payment)
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(background)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            AmountText.providerName(payment.provider),
            color = ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (payment.isSuspicious) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_warning), null, Modifier.size(28.dp), tint = ink)
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.suspicious),
                    color = ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        BigAmount(payment, ink)
        Text(AmountText.time(payment.receivedAt), color = ink, fontSize = 22.sp)
    }
}

/** "$45.25" on one line; shillings put "SLSH" above the number so big amounts still fit. */
@Composable
private fun BigAmount(payment: PaymentEntity, ink: Color) {
    val text = AmountText.format(payment)
    val (label, number) =
        when (payment.currency) {
            Currency.USD -> null to text
            Currency.SLSH -> "SLSH" to text.removePrefix("SLSH ")
        }
    if (label != null) Text(label, color = ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text(
        number,
        color = ink,
        fontSize = if (number.length > 9) 48.sp else 64.sp,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SayAgainButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(200.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CodkaColors.Ink,
            contentColor = Color.White
        )
    ) {
        Icon(
            painterResource(R.drawable.ic_speaker),
            stringResource(R.string.say_again),
            Modifier.size(110.dp)
        )
    }
}

@Composable
private fun NoPaymentYet(modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painterResource(R.drawable.ic_speaker),
                null,
                Modifier.size(140.dp),
                tint = Color(0xFFB0B0B0)
            )
            Text(
                stringResource(R.string.no_payment_yet),
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
