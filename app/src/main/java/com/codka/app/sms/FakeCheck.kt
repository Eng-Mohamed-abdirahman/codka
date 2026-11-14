package com.codka.app.sms

import com.codka.app.parser.ParsedPayment

/**
 * Decides whether a payment SMS looks fake. The real rules are task 13 (on hold): for example,
 * the new balance should be the last balance plus this payment.
 */
fun interface FakeCheck {
    sealed interface Verdict {
        data object Real : Verdict

        /** [reason] is saved with the payment so the history screen can explain the warning. */
        data class Fake(val reason: String) : Verdict
    }

    /** [lastBalance] is the balance the previous SMS for this account showed, if any. */
    fun check(payment: ParsedPayment, smsBody: String, lastBalance: String?): Verdict

    companion object {
        /** Until task 13: every payment the parser accepts is treated as real. */
        val NONE = FakeCheck { _, _, _ -> Verdict.Real }
    }
}
