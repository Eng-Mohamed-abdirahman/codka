package com.codka.app.sms

import com.codka.app.data.PaymentStore
import com.codka.app.data.Settings
import com.codka.app.parser.PaymentParser
import com.codka.app.voice.AnnounceRequest

/**
 * Task 19: what happens to every SMS. Parse → check → save → speak.
 *
 *  - A payment is saved, its balance remembered, and it is spoken.
 *  - The same SMS again is not saved or spoken a second time.
 *  - A Zaad / eDahab "money received" SMS that can't be read says "Fariin aan la garanayn".
 *  - A payment the fake check doubts is saved as suspicious and the warning is spoken.
 *  - Every other SMS is ignored silently, and nothing about it is kept.
 *
 * [speak] hands the announcement to the announcer; it is the only thing that touches Android,
 * so this class can be tested on the computer.
 */
class PaymentPipeline(
    private val parser: PaymentParser,
    private val store: PaymentStore,
    private val settings: Settings,
    private val fakeCheck: FakeCheck,
    private val speak: suspend (AnnounceRequest) -> Unit
) {
    sealed interface Outcome {
        /** Not a payment: nothing was saved or said. */
        data object Ignored : Outcome

        /** Looked like money received but couldn't be read: "Fariin aan la garanayn". */
        data object Unknown : Outcome

        /** Already saved earlier: nothing said again. */
        data object Duplicate : Outcome

        data class Payment(val id: Long) : Outcome

        data class Fake(val id: Long, val reason: String) : Outcome
    }

    suspend fun handle(smsBody: String, receivedAt: Long): Outcome {
        val payment = parser.parse(smsBody)
        if (payment == null) {
            if (!looksLikeMoneyReceived(smsBody)) return Outcome.Ignored
            speak(AnnounceRequest.Unknown)
            return Outcome.Unknown
        }

        val lastBalance = settings.lastBalance(payment.provider, payment.currency)
        val verdict = fakeCheck.check(payment, smsBody, lastBalance)
        val fake = verdict as? FakeCheck.Verdict.Fake
        val saved =
            store.save(
                payment,
                smsBody,
                receivedAt,
                isSuspicious = fake != null,
                reason = fake?.reason
            )
        return when (saved) {
            PaymentStore.SaveResult.Duplicate -> Outcome.Duplicate
            PaymentStore.SaveResult.BadAmount -> {
                speak(AnnounceRequest.Unknown)
                Outcome.Unknown
            }
            is PaymentStore.SaveResult.Saved ->
                if (fake != null) {
                    // A fake SMS's balance is made up: keep the last real one.
                    speak(AnnounceRequest.Fake)
                    Outcome.Fake(saved.id, fake.reason)
                } else {
                    payment.balance?.let {
                        settings.setLastBalance(payment.provider, payment.currency, it)
                    }
                    speak(
                        AnnounceRequest.Payment(
                            payment.currency,
                            payment.amount,
                            payment.senderPhone
                        )
                    )
                    Outcome.Payment(saved.id)
                }
        }
    }

    companion object {
        private val MONEY_RECEIVED = Regex("""ka\s+heshay""", RegexOption.IGNORE_CASE)

        /**
         * True for a Zaad / eDahab SMS that says money was received ("ka heshay"). Money sent,
         * top-ups and exchange never say it, so they stay silent even if the parser fails.
         */
        fun looksLikeMoneyReceived(body: String): Boolean =
            PaymentSmsFilter.providerOf(body) != null && MONEY_RECEIVED.containsMatchIn(body)
    }
}
