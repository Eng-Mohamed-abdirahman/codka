@file:OptIn(ExperimentalTextApi::class)

package com.codka.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codka.app.R
import com.codka.app.data.PaymentEntity
import com.codka.app.parser.Provider

/**
 * Codka's design system (the "Codka" design canvas, board 20 "Tokens & handoff").
 * Saffron and Indigo are the brand; green is only for "paid" and red only for "fake".
 * Light theme only: Cream reads best in sunlight.
 */
object CodkaColors {
    // brand
    val Saffron = Color(0xFFF2A900)
    val Saffron50 = Color(0xFFFFF4D6)
    val Saffron100 = Color(0xFFFFE7A3)
    val Saffron800 = Color(0xFF7A5600)
    val Indigo = Color(0xFF1E2A5A)
    val Indigo50 = Color(0xFFEEF0F7)
    val Indigo300 = Color(0xFF8C97C2)

    // neutral
    val Cream = Color(0xFFFFF8EC)
    val White = Color(0xFFFFFFFF)
    val Sand = Color(0xFFF3EAD8)
    val Line = Color(0xFFE2D6BE)
    val Muted = Color(0xFF5C5547)
    val Ink = Color(0xFF1A1A1A)

    // signal: only for paid / fake
    val Paid50 = Color(0xFFE6F5EC)
    val PaidLine = Color(0xFFBFE3CC)
    val Paid700 = Color(0xFF157A40)
    val Alert50 = Color(0xFFFDECEA)
    val AlertLine = Color(0xFFF3C1BC)
    val Alert700 = Color(0xFFB3261E)

    // provider: placeholders in the design, to be confirmed with the sellers
    val Zaad = Color(0xFF0E6E6D)
    val EDahab = Color(0xFF6B3590)

    fun of(provider: Provider) = when (provider) {
        Provider.ZAAD -> Zaad
        Provider.EDAHAB -> EDahab
    }

    /** The letters on a provider badge: no logos, just "Z" and "eD". */
    fun badge(provider: Provider) = when (provider) {
        Provider.ZAAD -> "Z"
        Provider.EDAHAB -> "eD"
    }

    /** A history row's badge colour: the provider's, or red if it may be fake. */
    fun markOf(payment: PaymentEntity) =
        if (payment.isSuspicious) Alert700 else of(payment.provider)
}

private val Baloo =
    FontFamily(
        Font(
            R.font.baloo2,
            FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700))
        ),
        Font(
            R.font.baloo2,
            FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(800))
        )
    )

private val Atkinson =
    FontFamily(
        Font(R.font.atkinson_regular, FontWeight.Normal),
        Font(R.font.atkinson_bold, FontWeight.Bold)
    )

/** Type scale. Amounts use Atkinson Hyperlegible: its digits are the easiest to tell apart. */
object CodkaType {
    val AmountXl =
        TextStyle(
            fontFamily = Atkinson,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
            lineHeight = 64.sp
        )
    val AmountL =
        TextStyle(
            fontFamily = Atkinson,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 44.sp
        )
    val Display =
        TextStyle(
            fontFamily = Baloo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 36.sp
        )
    val Title =
        TextStyle(
            fontFamily = Baloo,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp
        )
    val Heading =
        TextStyle(
            fontFamily = Baloo,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        )
    val Button =
        TextStyle(
            fontFamily = Atkinson,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 24.sp
        )
    val BodyL = TextStyle(fontFamily = Atkinson, fontSize = 18.sp, lineHeight = 26.sp)
    val Body = TextStyle(fontFamily = Atkinson, fontSize = 16.sp, lineHeight = 24.sp)
    val Caption = TextStyle(fontFamily = Atkinson, fontSize = 14.sp, lineHeight = 20.sp)
}

object CodkaShape {
    val Sm = RoundedCornerShape(8.dp)
    val Md = RoundedCornerShape(16.dp)
    val Lg = RoundedCornerShape(24.dp)
    val Badge = RoundedCornerShape(12.dp)
    val Pill = CircleShape
}

object CodkaSize {
    val TouchMin = 56.dp
    val ButtonH = 60.dp
    val Repeat = 168.dp
    val RepeatHalo = 12.dp
    val NavBar = 76.dp
    val Row = 76.dp
    val Play = 52.dp
    val Badge = 44.dp
}

@Composable
fun CodkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
        lightColorScheme(
            primary = CodkaColors.Saffron,
            onPrimary = CodkaColors.Ink,
            secondary = CodkaColors.Indigo,
            onSecondary = CodkaColors.Cream,
            background = CodkaColors.Cream,
            onBackground = CodkaColors.Ink,
            surface = CodkaColors.Cream,
            onSurface = CodkaColors.Ink,
            error = CodkaColors.Alert700,
            onError = CodkaColors.White,
            outline = CodkaColors.Line
        ),
        typography = Typography(bodyLarge = CodkaType.Body, bodyMedium = CodkaType.Body),
        content = content
    )
}
