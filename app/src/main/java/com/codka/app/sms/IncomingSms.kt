package com.codka.app.sms

/** One SMS as Codka sees it: who sent it, the text, and when the network says it was sent. */
data class IncomingSms(val sender: String, val body: String, val sentAtMillis: Long)

/**
 * A long SMS arrives as several parts. Joins the parts of each sender back into one message,
 * keeping the order they arrived in. Pure Kotlin so it can be unit tested.
 */
object SmsAssembler {
    fun assemble(parts: List<IncomingSms>): List<IncomingSms> = parts
        .groupBy { it.sender }
        .map { (sender, messageParts) ->
            IncomingSms(
                sender = sender,
                body = messageParts.joinToString("") { it.body },
                sentAtMillis = messageParts.first().sentAtMillis
            )
        }
}
