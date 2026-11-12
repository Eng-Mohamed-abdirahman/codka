package com.codka.app.parser

import android.content.Context

/** The app's one [PaymentParser], read from assets/parsers.json the first time it's needed. */
object AppParser {
    @Volatile private var parser: PaymentParser? = null

    fun get(context: Context): PaymentParser = parser ?: synchronized(this) {
        parser ?: PaymentParser(
            SmsFormatLoader.fromJson(
                context.assets.open(SmsFormatLoader.ASSET_FILE).bufferedReader().use {
                    it.readText()
                }
            )
        ).also { parser = it }
    }
}
