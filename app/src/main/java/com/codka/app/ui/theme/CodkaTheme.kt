package com.codka.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.codka.app.data.PaymentEntity
import com.codka.app.parser.Provider

/**
 * Codka's colours. Sellers may not read, so colour carries the meaning:
 * Zaad is green, eDahab is gold, and red always means "careful, this may be fake".
 */
object CodkaColors {
    val Zaad = Color(0xFF1B8A3A)
    val OnZaad = Color.White
    val Edahab = Color(0xFFE0A100)
    val OnEdahab = Color(0xFF2B2100)
    val Suspicious = Color(0xFFC62828)
    val OnSuspicious = Color.White
    val SuspiciousLight = Color(0xFFFDECEA)
    val Ink = Color(0xFF1C1B1F)
    val Paper = Color(0xFFFAFAF7)

    fun of(provider: Provider) = when (provider) {
        Provider.ZAAD -> Zaad
        Provider.EDAHAB -> Edahab
    }

    fun onColor(provider: Provider) = when (provider) {
        Provider.ZAAD -> OnZaad
        Provider.EDAHAB -> OnEdahab
    }

    /** A payment's colour: its provider's, or red if it may be fake. */
    fun cardOf(payment: PaymentEntity) =
        if (payment.isSuspicious) Suspicious else of(payment.provider)

    fun onCardOf(payment: PaymentEntity) =
        if (payment.isSuspicious) OnSuspicious else onColor(payment.provider)
}

@Composable
fun CodkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
        lightColorScheme(
            primary = CodkaColors.Zaad,
            onPrimary = CodkaColors.OnZaad,
            background = CodkaColors.Paper,
            surface = CodkaColors.Paper,
            onBackground = CodkaColors.Ink,
            onSurface = CodkaColors.Ink,
            error = CodkaColors.Suspicious
        ),
        content = content
    )
}
