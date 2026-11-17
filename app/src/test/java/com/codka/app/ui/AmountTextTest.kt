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
        "USD, 500, '$5.00'",
        "USD, 200000, '$2,000.00'",
        "USD, 50, '$0.50'",
        "SLSH, 2550000, '25,500 SLSH'",
        "SLSH, 125000000, '1,250,000 SLSH'",
        "SLSH, 3125080, '31,250.80 SLSH'"
    )
    fun `amounts are written as the design says`(currency: Currency, minor: Long, text: String) {
        assertEquals(text, AmountText.format(currency, minor))
    }

    @Test
    fun `the big amount is the number alone`() {
        assertEquals("10.50", AmountText.number(payment(1050)))
        assertEquals("25,500", AmountText.number(payment(2550000, Currency.SLSH)))
    }

    @Test
    fun `sender shows as the last 4 digits and a short name`() {
        assertEquals("…4521", AmountText.lastDigits("633004521"))
        assertEquals("…4521", AmountText.lastDigits("+252 63 300 4521"))
        assertNull(AmountText.lastDigits("12"))
        assertEquals("Axmed C.", AmountText.shortName("AXMED CALI FAARAX"))
        assertEquals("Hodan", AmountText.shortName("hodan"))
        assertNull(AmountText.shortName("  "))
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
    fun `replay says the amount, or the warning for a suspicious payment`() {
        assertEquals(
            AnnounceRequest.Payment(Currency.USD, "45.25"),
            Replay.requestFor(payment(4525))
        )
        assertEquals(AnnounceRequest.Fake, Replay.requestFor(payment(4525, suspicious = true)))
    }
}
