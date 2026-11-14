package com.codka.app.voice

import com.codka.app.parser.Currency

/** What the announcer has to say. Kept to plain values so it can travel in an Intent. */
sealed interface AnnounceRequest {
    data class Payment(val currency: Currency, val amount: String, val senderPhone: String) :
        AnnounceRequest

    /** The SMS looks like a fake payment message. */
    data object Fake : AnnounceRequest

    /** From Zaad / eDahab, but the format isn't recognised. */
    data object Unknown : AnnounceRequest
}

/**
 * Builds the sentence Codka plays, as a list of recorded clips in res/raw (task 18):
 *
 * "Lacag ayaa kuu timid" · amount · currency · "ka timid lambarka" · the sender's full number,
 * digit by digit
 *
 * then "Mar kale." and the whole sentence again.
 */
object Announcement {
    const val ARRIVED = "p_lacag_ayaa_kuu_timid"
    const val FROM_NUMBER = "p_ka_timid_lambarka"
    const val ONCE_MORE = "p_mar_kale"
    const val FAKE = "p_digniin_been_abuur"
    const val UNKNOWN = "p_fariin_aan_la_garanayn"
    const val TEST = "p_tijaabo"

    /** Shorter than this is not a real phone number, so it isn't read. */
    private const val MIN_DIGITS = 4

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
        val clips = mutableListOf(ARRIVED)
        amount.mapTo(clips, VoiceClips::forWord)

        val number = localNumber(p.senderPhone)
        if (number.length >= MIN_DIGITS) {
            clips += FROM_NUMBER
            SomaliNumbers.digits(number).mapTo(clips, VoiceClips::forWord)
        }
        // No usable number: the amount alone is still the important part.
        return clips
    }

    /** "+252 63 456 1234" -> "634561234": digits only, without the Somalia code 252. */
    fun localNumber(phone: String): String {
        val digits = phone.filter { it in '0'..'9' }
        return if (digits.length > 9 && digits.startsWith("252")) digits.drop(3) else digits
    }
}
