package com.codka.app.ui.home

import androidx.compose.ui.unit.sp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AmountSizeTest {
    @Test
    fun `short amounts are 64 sp`() {
        assertEquals(64.sp, amountSize("$10.50", usd = true))
        assertEquals(64.sp, amountSize("500", usd = false))
    }

    @Test
    fun `long amounts step down so they never wrap`() {
        assertEquals(52.sp, amountSize("$1,250.50", usd = true))
        assertEquals(52.sp, amountSize("25,500", usd = false))
        assertEquals(52.sp, amountSize("150,000", usd = false))
        assertEquals(44.sp, amountSize("1,250,000", usd = false))
    }
}
