package com.codka.app.voice

import com.codka.app.parser.Currency

/**
 * Turns numbers into the Somali words Codka plays, one word per voice clip.
 *
 * Word order is **ones first** ("shan iyo labaatan" for 25), as the market sellers say it.
 * Rules (docs/voice-recording-guide.md):
 * - 100 = "boqol" and 1,000 = "kun" on their own; 1,000,000 = "hal malyuun".
 * - 200 = "laba boqol", 3,000 = "saddex kun": the number comes before boqol / kun / malyuun.
 * - Parts are joined with "iyo": 1,250,000 = "hal malyuun iyo laba boqol iyo konton kun".
 * - A whole amount of 1 is "hal" (hal doolar). Inside a bigger number 1 is "kow" (boqol iyo kow).
 * - Phone digits are read one by one, 1 = "kow" and 0 = "eber".
 */
object SomaliNumbers {
    const val MAX = 999_999_999L

    private val ONES =
        listOf("", "kow", "laba", "saddex", "afar", "shan", "lix", "toddoba", "siddeed", "sagaal")
    private val TENS =
        listOf(
            "",
            "toban",
            "labaatan",
            "soddon",
            "afartan",
            "konton",
            "lixdan",
            "toddobaatan",
            "siddeetan",
            "sagaashan"
        )
    private const val HAL = "hal"
    private const val ZERO = "eber"
    private const val AND = "iyo"
    private const val HUNDRED = "boqol"
    private const val THOUSAND = "kun"
    private const val MILLION = "malyuun"
    private const val CENTS = "senti"

    /** Words for [n] (0..[MAX]). Throws if [n] is out of range. */
    fun words(n: Long): List<String> = requireNotNull(wordsOrNull(n)) { "$n is outside 0..$MAX" }

    /** Words for [n], or null if it is outside 0..[MAX]. */
    fun wordsOrNull(n: Long): List<String>? = when {
        n !in 0..MAX -> null
        n == 0L -> listOf(ZERO)
        n == 1L -> listOf(HAL)
        else -> joinParts(n)
    }

    /**
     * Words for a money amount exactly as the parser gave it ("7.5", "45.25", "2000"),
     * followed by the currency word, then "iyo ... senti" if there are cents.
     *
     * Returns null when the amount can't be spoken exactly (not a plain number, more than
     * 2 decimals, zero, or too big). Never rounds.
     */
    fun amount(amount: String, currency: Currency): List<String>? {
        val match = AMOUNT.matchEntire(amount) ?: return null
        val whole = match.groupValues[1].toLongOrNull() ?: return null
        val cents = match.groupValues[2].padEnd(2, '0').toInt()
        if (whole > MAX || (whole == 0L && cents == 0)) return null

        val currencyWord =
            when (currency) {
                Currency.USD -> "doolar"
                Currency.SLSH -> "shilin"
            }
        val centWords = if (cents > 0) wordsOrNull(cents.toLong())!! + CENTS else emptyList()
        return when {
            whole == 0L -> centWords
            cents == 0 -> words(whole) + currencyWord
            else -> words(whole) + currencyWord + AND + centWords
        }
    }

    /** Each digit of [number] as a word ("6001" -> lix eber eber kow). Non-digits are skipped. */
    fun digits(number: String): List<String> =
        number.filter { it in '0'..'9' }.map { c -> if (c == '0') ZERO else ONES[c - '0'] }

    // Whole part, then an optional dot with up to 2 decimals ("8." and "8.00" are allowed).
    private val AMOUNT = Regex("""(\d+)(?:\.(\d{0,2}))?""")

    /** n is 2..MAX. Splits into millions, thousands and the rest, joined with "iyo". */
    private fun joinParts(n: Long): List<String> {
        val millions = (n / 1_000_000).toInt()
        val thousands = (n / 1_000 % 1_000).toInt()
        val rest = (n % 1_000).toInt()

        val parts = mutableListOf<List<String>>()
        if (millions >
            0
        ) {
            parts += (if (millions == 1) listOf(HAL) else below1000(millions)) + MILLION
        }
        if (thousands >
            0
        ) {
            parts += (if (thousands == 1) emptyList() else below1000(thousands)) + THOUSAND
        }
        if (rest > 0) parts += below1000(rest)
        return parts.flatMapIndexed { i, part -> if (i == 0) part else listOf(AND) + part }
    }

    /** n is 1..999. */
    private fun below1000(n: Int): List<String> {
        val hundreds = n / 100
        val rest = n % 100
        val parts = mutableListOf<List<String>>()
        if (hundreds >
            0
        ) {
            parts += (if (hundreds == 1) emptyList() else listOf(ONES[hundreds])) + HUNDRED
        }
        if (rest > 0) parts += below100(rest)
        return parts.flatMapIndexed { i, part -> if (i == 0) part else listOf(AND) + part }
    }

    /** n is 1..99: "shan iyo labaatan" (ones first). */
    private fun below100(n: Int): List<String> {
        val tens = n / 10
        val ones = n % 10
        return when {
            tens == 0 -> listOf(ONES[ones])
            ones == 0 -> listOf(TENS[tens])
            else -> listOf(ONES[ones], AND, TENS[tens])
        }
    }
}
