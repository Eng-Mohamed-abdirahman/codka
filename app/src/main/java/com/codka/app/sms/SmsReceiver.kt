package com.codka.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Telephony
import android.util.Log
import com.codka.app.parser.AppParser

/**
 * Task 09: Codka's SMS listener. Android starts it for every incoming SMS, even when the app
 * is closed, the screen is locked or the phone has just restarted (proven in the task 07 spike).
 *
 * Every SMS goes to the payment parser. Only money *received* on Zaad / eDahab gets through;
 * money sent, top-ups, exchange and every other SMS are dropped right here: not logged in
 * release builds, not stored, nothing about them is kept.
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

        val parser = AppParser.get(context.applicationContext)
        val payments =
            SmsAssembler.assemble(parts).mapNotNull { sms ->
                if (isDebugBuild(context)) logRawSample(sms)
                parser.parse(sms.body)?.let { sms to it }
            }
        if (payments.isEmpty()) return

        // Keep the receiver alive until the work (the beep) is finished.
        val pending = goAsync()
        var remaining = payments.size
        payments.forEach { (sms, payment) ->
            PaymentHandler.onPayment(context.applicationContext, sms, payment, receivedAt) {
                remaining--
                if (remaining == 0) pending.finish()
            }
        }
    }

    private fun isDebugBuild(context: Context) =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /**
     * Debug builds only: print the full text of Zaad / eDahab SMS to logcat, so new wordings
     * can be copied into the test samples (anonymise them first). Personal SMS are never printed.
     */
    private fun logRawSample(sms: IncomingSms) {
        if (PaymentSmsFilter.providerOf(sms.body) == null) return
        Log.d(PaymentHandler.TAG, "RAW SMS from '${sms.sender}': ${sms.body}")
    }
}
