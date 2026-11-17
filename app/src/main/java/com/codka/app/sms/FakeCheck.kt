package com.codka.app.sms

import com.codka.app.parser.Currency
import com.codka.app.parser.ParsedPayment
import com.codka.app.parser.Provider
import java.math.BigDecimal

/** Decides whether a payment SMS looks fake (task 13). */
fun interface FakeCheck {
    sealed interface Verdict {
        data object Real : Verdict

        /** [reason] is saved with the payment so the history screen can explain the warning. */
        data class Fake(val reason: String) : Verdict
    }

    /** [sender] is who the phone says sent the SMS ("222", "eDahab-USD", or a phone number). */
    fun check(payment: ParsedPayment, sender: String): Verdict

    companion object {
        /** Every payment the parser accepts is treated as real. For tests. */
        val NONE = FakeCheck { _, _ -> Verdict.Real }

        /** Reasons saved with a fake payment. */
        const val WRONG_SENDER = "wrong_sender"
        const val BALANCE_BELOW_AMOUNT = "balance_below_amount"

        /**
         * The sender ID each account's real SMS come from. Anyone can copy the wording of a
         * payment SMS, but a phone number can't send as "222" or "eDahab-USD".
         */
        fun officialSender(provider: Provider, currency: Currency): String = when (provider) {
            Provider.ZAAD -> if (currency == Currency.SLSH) "222" else "898"
            Provider.EDAHAB -> if (currency == Currency.SLSH) "eDahab-SHL" else "eDahab-USD"
        }

        /**
         * The rules Codka uses:
         *  - The SMS must come from the official sender ID of its account (see [officialSender]).
         *  - The new balance can't be smaller than the money just received.
         *
         * It does not check that the balance went up by exactly the amount: when the seller sends
         * money or tops up, the balance drops between payments, and that would be a false alarm.
         */
        val RULES = FakeCheck { payment, sender ->
            val official = officialSender(payment.provider, payment.currency)
            when {
                !sender.trim().equals(official, ignoreCase = true) -> Verdict.Fake(WRONG_SENDER)
                balanceBelowAmount(payment) -> Verdict.Fake(BALANCE_BELOW_AMOUNT)
                else -> Verdict.Real
            }
        }

        private fun balanceBelowAmount(payment: ParsedPayment): Boolean {
            val balance = payment.balance?.let(::decimal) ?: return false
            val amount = decimal(payment.amount) ?: return false
            return balance < amount
        }

        // Exact, never a floating-point number: "150.0049" stays 150.0049.
        private fun decimal(text: String): BigDecimal? =
            text.replace(",", "").trim().toBigDecimalOrNull()
    }
}
