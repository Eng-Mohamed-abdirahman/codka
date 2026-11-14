package com.codka.app.data

import java.util.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DayOfTest {
    private val eat = TimeZone.getTimeZone("Africa/Mogadishu")

    @Test
    fun `the day runs from local midnight to the next midnight`() {
        // 14 Nov 2026 09:00 in Somaliland (UTC+3) = 06:00 UTC.
        val morning = 1_794_636_000_000L
        val (from, until) = PaymentStore.dayOf(morning, eat)
        // 14 Nov 2026 00:00 in Somaliland = 13 Nov 21:00 UTC.
        assertEquals(1_794_603_600_000L, from)
        assertEquals(from + 24 * 3_600_000L, until)
    }

    @Test
    fun `midnight itself starts a new day`() {
        val midnight = 1_794_603_600_000L
        assertEquals(midnight, PaymentStore.dayOf(midnight, eat).first)
        assertEquals(midnight - 24 * 3_600_000L, PaymentStore.dayOf(midnight - 1, eat).first)
    }
}
