package com.codka.app.voice

import com.codka.app.parser.Currency

/** What the announcer has to say. Kept to plain values so it can travel in an Intent. */
sealed interface AnnounceRequest {
    data class Payment(val currency: Currency, val amount: String) : AnnounceRequest

    /** The SMS looks like a fake payment message. */
    data object Fake : AnnounceRequest

    /** From Zaad / eDahab, but the format isn't recognised. */
    data object Unknown : AnnounceRequest
}

/**
 * Builds the sentence Codka plays, as a list of recorded clips in res/raw (task 18):
 *
 * "Lacag ayaa kuu timid" · amount · currency
 *
 * then "Mar kale." and the whole sentence again (about 20 seconds). The sender's number is not
 * spoken: it made each payment last close to a minute. It is still saved for the history screen.
 */
object Announcement {
    const val ARRIVED = "p_lacag_ayaa_kuu_timid"
    const val ONCE_MORE = "p_mar_kale"
    const val FAKE = "p_digniin_been_abuur"
    const val UNKNOWN = "p_fariin_aan_la_garanayn"
    const val TEST = "p_tijaabo"

    /** The full announcement, spoken twice. */
    fun build(request: AnnounceRequest): List<String> {
        val once = once(request)
        return once + ONCE_MORE + once
    }

    fun once(request: AnnounceRequest): List<String> = when (request) {
        AnnounceRequest.Fake -> listOf(FAKE)
        AnnounceRequest.Unknown -> listOf(UNKNOWN)
        is AnnounceRequest.Payment -> payment(request)
    }

    private fun payment(p: AnnounceRequest.Payment): List<String> {
        // An amount we can't say exactly is never guessed: say "unknown message" instead.
        val amount = SomaliNumbers.amount(p.amount, p.currency) ?: return listOf(UNKNOWN)
        return listOf(ARRIVED) + amount.map(VoiceClips::forWord)
    }
}
