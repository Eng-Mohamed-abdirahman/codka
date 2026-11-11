package com.codka.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Task 09: Codka's SMS listener. Android starts it for every incoming SMS, even when the app
 * is closed, the screen is locked or the phone has just restarted (proven in the task 07 spike).
 *
 * Privacy: only Zaad / eDahab payment SMS are handled. Every other SMS is dropped right here:
 * not logged, not stored, nothing about it is kept.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val receivedAt = System.currentTimeMillis()
        val parts =
            Telephony.Sms.Intents
                .getMessagesFromIntent(intent)
                ?.map {
                    IncomingSms(
                        it.originatingAddress.orEmpty(),
                        it.messageBody.orEmpty(),
                        it.timestampMillis
                    )
                }
                .orEmpty()

        val payments = SmsAssembler.assemble(parts).filter { PaymentSmsFilter.isPayment(it.body) }
        if (payments.isEmpty()) return

        // Keep the receiver alive until the work (the beep) is finished.
        val pending = goAsync()
        var remaining = payments.size
        payments.forEach { sms ->
            PaymentHandler.onPayment(context.applicationContext, sms, receivedAt) {
                remaining--
                if (remaining == 0) pending.finish()
            }
        }
    }
}
