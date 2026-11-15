package com.codka.app.ui.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.codka.app.ui.AmountText
import com.codka.app.ui.theme.CodkaColors

/**
 * Task 21: today's payments, newest first. Each row shows the provider's colour, the amount,
 * the time and a play button. A payment that may be fake is red with a warning sign.
 */
@Composable
fun HistoryScreen(today: List<PaymentEntity>, onPlay: (PaymentEntity) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.history_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        AmountText.totals(today)?.let {
            Text(
                stringResource(R.string.today_total, it),
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }

        if (today.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.today_none),
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(today, key = { it.id }) { payment -> PaymentRow(payment, onPlay) }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: PaymentEntity, onPlay: (PaymentEntity) -> Unit) {
    val stripe = CodkaColors.cardOf(payment)
    val rowBackground = if (payment.isSuspicious) CodkaColors.SuspiciousLight else Color.White
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(rowBackground)
            .height(88.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The provider's colour down the left side: green Zaad, gold eDahab, red if suspicious.
        Box(Modifier.width(14.dp).fillMaxSize().background(stripe))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (payment.isSuspicious) {
                    Icon(
                        painterResource(R.drawable.ic_warning),
                        stringResource(R.string.suspicious),
                        Modifier.size(24.dp),
                        tint = CodkaColors.Suspicious
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    AmountText.format(payment),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (payment.isSuspicious) CodkaColors.Suspicious else CodkaColors.Ink,
                    maxLines = 1
                )
            }
            Text(
                "${AmountText.time(
                    payment.receivedAt
                )} · ${AmountText.providerName(payment.provider)}",
                fontSize = 18.sp,
                color = Color(0xFF555555)
            )
        }
        FilledIconButton(
            onClick = { onPlay(payment) },
            modifier = Modifier.padding(end = 12.dp).size(64.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = CodkaColors.Ink)
        ) {
            Icon(
                painterResource(R.drawable.ic_play),
                stringResource(R.string.play),
                Modifier.size(40.dp),
                tint = Color.White
            )
        }
    }
}
