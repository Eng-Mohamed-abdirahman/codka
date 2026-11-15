package com.codka.app.ui

import com.codka.app.data.PaymentEntity
import com.codka.app.parser.Currency
import com.codka.app.parser.Provider
import com.codka.app.voice.AnnounceRequest
import java.util.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AmountTextTest {
    private fun payment(
        minor: Long,
        currency: Currency = Currency.USD,
        suspicious: Boolean = false
    ) = PaymentEntity(
        provider = Provider.ZAAD,
        currency = currency,
        amountMinor = minor,
        senderName = "AXMED CALI",
        senderPhone = "633000001",
        receivedAt = 0,
        isSuspicious = suspicious,
        smsHash = "x$minor$currency$suspicious"
    )

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource(
        "USD, 4525, '$45.25'",
        "USD, 705, '$7.05'",
        "USD, 200000, '$2,000'",
        "USD, 50, '$0.50'",
        "SLSH, 125000000, 'SLSH 1,250,000'",
        "SLSH, 3125080, 'SLSH 31,250.80'"
    )
    fun `amounts are written with thousands commas`(currency: Currency, minor: Long, text: String) {
        assertEquals(text, AmountText.format(currency, minor))
    }

    @Test
    fun `time is the 24-hour clock in the phone's time zone`() {
        // 14 Nov 2026 09:05 in Somaliland (UTC+3).
        assertEquals(
            "09:05",
            AmountText.time(1_794_636_300_000L, TimeZone.getTimeZone("Africa/Mogadishu"))
        )
    }

    @Test
    fun `totals add up each currency and leave out suspicious payments`() {
        val today =
            listOf(
                payment(4525),
                payment(12000),
                payment(99900, suspicious = true),
                payment(2500000, Currency.SLSH)
            )
        assertEquals("$165.25 · SLSH 25,000", AmountText.totals(today))
    }

    @Test
    fun `no real payments means no total`() {
        assertNull(AmountText.totals(emptyList()))
        assertNull(AmountText.totals(listOf(payment(500, suspicious = true))))
    }

    @Test
    fun `replay says the amount, or the warning for a suspicious payment`() {
        assertEquals(
            AnnounceRequest.Payment(Currency.USD, "45.25"),
            Replay.requestFor(payment(4525))
        )
        assertEquals(AnnounceRequest.Fake, Replay.requestFor(payment(4525, suspicious = true)))
    }
}
