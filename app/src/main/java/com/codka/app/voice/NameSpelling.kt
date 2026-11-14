package com.codka.app.voice

/**
 * Rewrites a Somali name so a **Swahili** TTS voice says it close to the Somali way.
 *
 * Phones have no Somali voice, but Swahili, like Somali, reads words the way they are written.
 * Only the letters Swahili reads differently are changed:
 *  - x -> h  (Axmed -> Ahmed)
 *  - c -> dropped  (Cali -> Ali; the Somali sound has no Swahili letter)
 *  - dh -> d  (Swahili "dh" is the English "th")
 *  - q -> k  (Swahili has no q)
 *  - ' -> dropped
 */
object NameSpelling {
    fun forSwahili(name: String): String? {
        val words =
            name
                .trim()
                .split(Regex("\\s+"))
                .map(::word)
                .filter { it.isNotEmpty() }
        return words.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun word(raw: String): String {
        val w =
            raw
                .lowercase()
                .replace("dh", "d")
                .replace("x", "h")
                .replace("c", "")
                .replace("q", "k")
                .filter { it.isLetter() || it == '-' }
                // "Sacaad" would become "saaad": keep at most two of the same vowel.
                .replace(Regex("([aeiou])\\1{2,}"), "$1$1")
                .trim('-')
        return w.replaceFirstChar { it.uppercase() }
    }
}
