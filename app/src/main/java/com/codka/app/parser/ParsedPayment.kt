package com.codka.app.parser

enum class Provider { ZAAD, EDAHAB }

enum class Currency { USD, SLSH }

/**
 * What Codka needs to speak a payment.
 *
 * [amount] is exactly what the SMS says, with only the thousands commas removed
 * ("12.5", "45.25", "2000"). It is never rounded and never turned into a floating-point number.
 */
data class ParsedPayment(
    val provider: Provider,
    val currency: Currency,
    val amount: String,
    val senderName: String,
    val senderPhone: String
)
