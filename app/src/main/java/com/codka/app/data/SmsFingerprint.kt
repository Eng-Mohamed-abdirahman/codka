package com.codka.app.data

import java.security.MessageDigest

/**
 * A short fingerprint of an SMS, so the same SMS is never saved twice (for example when
 * Android delivers it again). Each real payment SMS has its own transaction number and time,
 * so two real payments never share a fingerprint, even with the same amount and sender.
 * Only the fingerprint is stored, not the SMS text.
 */
object SmsFingerprint {
    private val WHITESPACE = Regex("""\s+""")

    fun of(body: String): String {
        // A long SMS can arrive in parts with different spacing: ignore spacing differences.
        val text = body.trim().replace(WHITESPACE, " ")
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
