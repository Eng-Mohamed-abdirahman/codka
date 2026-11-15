package com.codka.app.ui

import android.content.Context
import com.codka.app.data.PaymentEntity
import com.codka.app.voice.AnnounceRequest
import com.codka.app.voice.AnnouncerService

/** Says a saved payment again. A suspicious one replays the warning, never the amount. */
object Replay {
    fun requestFor(payment: PaymentEntity): AnnounceRequest = if (payment.isSuspicious) {
        AnnounceRequest.Fake
    } else {
        AnnounceRequest.Payment(payment.currency, payment.amount)
    }

    fun say(context: Context, payment: PaymentEntity) =
        AnnouncerService.announce(context, requestFor(payment))
}
