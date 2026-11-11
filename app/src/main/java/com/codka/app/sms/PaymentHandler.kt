package com.codka.app.sms

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * What Codka does with a payment SMS. For now: log it and beep.
 * Task 19 replaces the beep with: parse → save → speak in Somali.
 */
object PaymentHandler {
    const val TAG = "Codka"
    private const val BEEP_MS = 700

    /** Called on the main thread. Calls [done] when the work is finished. */
    fun onPayment(context: Context, sms: IncomingSms, receivedAtMillis: Long, done: () -> Unit) {
        val provider = PaymentSmsFilter.providerOf(sms.body)
        val delayMs = receivedAtMillis - sms.sentAtMillis
        val line = "PAYMENT ${provider?.name} from '${sms.sender}' delay=${delayMs}ms"
        Log.i(TAG, line)
        SpikeLog.add(context, line)
        beep(done)
    }

    private fun beep(done: () -> Unit) {
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, BEEP_MS)
        Handler(Looper.getMainLooper()).postDelayed({
            tone.release()
            done()
        }, BEEP_MS + 200L)
    }
}
