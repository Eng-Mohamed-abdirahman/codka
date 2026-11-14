package com.codka.app.sms

import android.content.Context
import android.util.Log
import com.codka.app.data.CodkaDatabase
import com.codka.app.data.PaymentStore
import com.codka.app.data.Settings
import com.codka.app.parser.AppParser
import com.codka.app.voice.AnnouncerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Task 19: runs each Zaad / eDahab SMS through the [PaymentPipeline] (parse → check → save →
 * speak), off the main thread so the database never slows down the phone.
 */
object PaymentHandler {
    const val TAG = "Codka"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var pipeline: PaymentPipeline? = null

    private fun pipeline(context: Context): PaymentPipeline = pipeline ?: synchronized(this) {
        pipeline ?: run {
            val app = context.applicationContext
            val db = CodkaDatabase.get(app)
            PaymentPipeline(
                parser = AppParser.get(app),
                store = PaymentStore(db.payments()),
                settings = Settings(db.settings()),
                fakeCheck = FakeCheck.NONE,
                // The announcer lives on the main thread.
                speak = { request ->
                    withContext(Dispatchers.Main) { AnnouncerService.announce(app, request) }
                }
            ).also { pipeline = it }
        }
    }

    /** Handles one SMS in the background and calls [done] when finished (also after an error). */
    fun onSms(context: Context, sms: IncomingSms, receivedAt: Long, done: () -> Unit) {
        val app = context.applicationContext
        scope.launch {
            try {
                val outcome = pipeline(app).handle(sms.body, receivedAt)
                if (outcome != PaymentPipeline.Outcome.Ignored) {
                    // No amount, name or number in the log: the outcome is enough to check it worked.
                    val delay = receivedAt - sms.sentAtMillis
                    val line = "${describe(outcome)} from '${sms.sender}' delay=${delay}ms"
                    Log.i(TAG, line)
                    SpikeLog.add(app, line)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not handle a payment SMS", e)
            } finally {
                done()
            }
        }
    }

    private fun describe(outcome: PaymentPipeline.Outcome) = when (outcome) {
        is PaymentPipeline.Outcome.Payment -> "PAYMENT saved #${outcome.id}"
        is PaymentPipeline.Outcome.Fake -> "FAKE saved #${outcome.id} (${outcome.reason})"
        PaymentPipeline.Outcome.Duplicate -> "DUPLICATE ignored"
        PaymentPipeline.Outcome.Unknown -> "UNKNOWN message"
        PaymentPipeline.Outcome.Ignored -> "ignored"
    }
}
