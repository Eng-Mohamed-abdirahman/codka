package com.codka.app.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class MoneyTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "45.25, 4525",
        "12.5, 1250",
        "7.05, 705",
        "8., 800",
        "2000, 200000",
        "'2,000', 200000",
        "1250000, 125000000",
        "0.5, 50"
    )
    fun `amounts become minor units exactly`(amount: String, minor: Long) {
        assertEquals(minor, Money.toMinor(amount))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "abc", "1.234", "-5", "1e3", ".5", "99999999999999999999"])
    fun `amounts that are not exact give null`(amount: String) {
        assertNull(Money.toMinor(amount))
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource("4525, 45.25", "1250, 12.50", "705, 7.05", "200000, 2000", "50, 0.50")
    fun `minor units back to text`(minor: Long, amount: String) {
        assertEquals(amount, Money.fromMinor(minor))
    }

    @ParameterizedTest
    @ValueSource(strings = ["45.25", "12.5", "2000", "0.05", "1250000"])
    fun `a round trip keeps the same money`(amount: String) {
        val back = Money.fromMinor(Money.toMinor(amount)!!)
        assertEquals(Money.toMinor(amount), Money.toMinor(back))
    }
}
