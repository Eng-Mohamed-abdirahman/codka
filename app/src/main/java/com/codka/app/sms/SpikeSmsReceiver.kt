package com.codka.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log

/**
 * Task 07 spike (throw-away): proves Android delivers payment SMS to Codka while the
 * app is closed, the screen is locked and after a restart. On a payment SMS it beeps and
 * logs the provider, the sender address and the delay. No parsing, no voice yet.
 */
class SpikeSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val receivedAt = System.currentTimeMillis()
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        // A long SMS arrives in several parts: join them per sender.
        parts.groupBy { it.originatingAddress.orEmpty() }.forEach { (sender, messages) ->
            val body = messages.joinToString("") { it.messageBody.orEmpty() }
            val provider = PaymentSmsFilter.providerOf(body)
            val delayMs = receivedAt - messages.first().timestampMillis
            val line =
                if (PaymentSmsFilter.isPayment(body)) {
                    "PAYMENT ${provider?.name} from '$sender' delay=${delayMs}ms"
                } else {
                    "other SMS from '$sender' (ignored)"
                }
            Log.i(TAG, line)
            SpikeLog.add(context.applicationContext, line)
            if (PaymentSmsFilter.isPayment(body)) beep(this)
        }
    }

    private fun beep(receiver: BroadcastReceiver) {
        val pending = receiver.goAsync()
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, BEEP_MS)
        Handler(Looper.getMainLooper()).postDelayed({
            tone.release()
            pending.finish()
        }, BEEP_MS + 200L)
    }

    companion object {
        const val TAG = "CodkaSpike"
        private const val BEEP_MS = 700
    }
}
