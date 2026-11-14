package com.codka.app.parser

import org.json.JSONObject

/** One payment SMS format from assets/parsers.json (task 10). */
data class SmsFormat(
    val id: String,
    val provider: Provider,
    val currency: Currency,
    val marker: String,
    val pattern: Regex,
    /** Finds the new balance (1 group). Null if this format has none. */
    val balancePattern: Regex? = null
)

object SmsFormatLoader {
    const val ASSET_FILE = "parsers.json"

    /** Reads the formats from the JSON text of parsers.json. Throws if the file is broken. */
    fun fromJson(json: String): List<SmsFormat> {
        val formats = JSONObject(json).getJSONArray("formats")
        return (0 until formats.length()).map { i ->
            val f = formats.getJSONObject(i)
            SmsFormat(
                id = f.getString("id"),
                provider = Provider.valueOf(f.getString("provider")),
                currency = Currency.valueOf(f.getString("currency")),
                marker = f.getString("marker"),
                pattern = Regex(f.getString("pattern")),
                balancePattern = f.optString("balancePattern").takeIf {
                    it.isNotEmpty()
                }?.let(::Regex)
            )
        }
    }
}
