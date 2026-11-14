package com.codka.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Telephony
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Task 09: Codka's SMS listener. Android starts it for every incoming SMS, even when the app
 * is closed, the screen is locked or the phone has just restarted (proven in the task 07 spike).
 *
 * Only Zaad / eDahab SMS go on to the [PaymentPipeline] (task 19), which saves and speaks
 * money *received*. Money sent, top-ups, exchange and every other SMS are dropped: not logged
 * in release builds, not stored, nothing about them is kept.
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

        // Only Zaad / eDahab SMS go further; everything else is dropped right here.
        val providerSms =
            SmsAssembler.assemble(parts).filter { sms ->
                if (isDebugBuild(context)) logRawSample(sms)
                PaymentSmsFilter.providerOf(sms.body) != null
            }
        if (providerSms.isEmpty()) return

        // Keep the receiver alive until each SMS is saved and handed to the announcer.
        val pending = goAsync()
        val remaining = AtomicInteger(providerSms.size)
        providerSms.forEach { sms ->
            PaymentHandler.onSms(context, sms, receivedAt) {
                if (remaining.decrementAndGet() == 0) pending.finish()
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
