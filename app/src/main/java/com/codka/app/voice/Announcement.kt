package com.codka.app.voice

import com.codka.app.parser.Currency

/** One piece of an announcement: a recorded clip, or the sender's name spoken by the phone's TTS voice. */
sealed interface Part {
    /** A file in res/raw, by name without ".ogg" (for example "n_5_shan"). */
    data class Clip(val name: String) : Part

    /** The sender's name, spoken by text-to-speech ("Axmed Cali"). */
    data class Name(val text: String) : Part
}

/** What the announcer has to say. Kept to plain values so it can travel in an Intent. */
sealed interface AnnounceRequest {
    data class Payment(
        val currency: Currency,
        val amount: String,
        val senderName: String,
        val senderPhone: String
    ) : AnnounceRequest

    /** The SMS looks like a fake payment message. */
    data object Fake : AnnounceRequest

    /** From Zaad / eDahab, but the format isn't recognised. */
    data object Unknown : AnnounceRequest
}

/**
 * Builds the sentence Codka plays, as a list of clips (task 18):
 *
 * "Lacag ayaa kuu timid" · amount · currency · "waxaa soo diray" · *name*
 * or, with no name (or no voice for names): … · "ka timid lambarka" · last 4 digits
 *
 * then "Mar kale." and the whole sentence again.
 */
object Announcement {
    const val ARRIVED = "p_lacag_ayaa_kuu_timid"
    const val SENT_BY = "p_waxaa_soo_diray"
    const val FROM_NUMBER = "p_ka_timid_lambarka"
    const val ONCE_MORE = "p_mar_kale"
    const val FAKE = "p_digniin_been_abuur"
    const val UNKNOWN = "p_fariin_aan_la_garanayn"
    const val TEST = "p_tijaabo"

    /**
     * The full announcement, spoken twice.
     * [canSpeakName] is false when the phone has no voice that can read the name;
     * then the last 4 digits of the number are read instead.
     */
    fun build(request: AnnounceRequest, canSpeakName: Boolean): List<Part> {
        val once = once(request, canSpeakName)
        return once + Part.Clip(ONCE_MORE) + once
    }

    /** True if [build] would speak the sender's name with TTS (so the name has to be prepared first). */
    fun wantsName(request: AnnounceRequest): Boolean =
        request is AnnounceRequest.Payment && displayName(request.senderName) != null

    fun once(request: AnnounceRequest, canSpeakName: Boolean): List<Part> = when (request) {
        AnnounceRequest.Fake -> listOf(Part.Clip(FAKE))
        AnnounceRequest.Unknown -> listOf(Part.Clip(UNKNOWN))
        is AnnounceRequest.Payment -> payment(request, canSpeakName)
    }

    private fun payment(p: AnnounceRequest.Payment, canSpeakName: Boolean): List<Part> {
        // An amount we can't say exactly is never guessed: say "unknown message" instead.
        val amount = SomaliNumbers.amount(p.amount, p.currency) ?: return listOf(Part.Clip(UNKNOWN))
        val parts = mutableListOf<Part>(Part.Clip(ARRIVED))
        amount.mapTo(parts) { Part.Clip(VoiceClips.forWord(it)) }

        val name = displayName(p.senderName)
        val lastDigits = p.senderPhone.filter { it in '0'..'9' }.takeLast(4)
        when {
            name != null && canSpeakName -> parts += listOf(Part.Clip(SENT_BY), Part.Name(name))
            lastDigits.length == 4 -> {
                parts += Part.Clip(FROM_NUMBER)
                SomaliNumbers.digits(lastDigits).mapTo(parts) { Part.Clip(VoiceClips.forWord(it)) }
            }
            // No usable name or number: the amount alone is still the important part.
        }
        return parts
    }

    /** "AXMED CALI FAARAX" -> "Axmed Cali Faarax", so TTS reads it as a name, not letter by letter. */
    fun displayName(raw: String): String? {
        val words = raw.trim().split(Regex("\\s+")).filter { w -> w.any { it.isLetter() } }
        if (words.isEmpty()) return null
        return words.joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
    }
}
