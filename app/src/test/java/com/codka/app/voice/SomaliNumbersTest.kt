package com.codka.app.voice

import com.codka.app.parser.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class SomaliNumbersTest {
    private fun words(n: Long) = SomaliNumbers.words(n).joinToString(" ")

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        "1, hal",
        "2, laba",
        "9, sagaal",
        "10, toban",
        "11, kow iyo toban",
        "12, laba iyo toban",
        "19, sagaal iyo toban",
        "20, labaatan",
        "21, kow iyo labaatan",
        "25, shan iyo labaatan",
        "45, shan iyo afartan",
        "99, sagaal iyo sagaashan",
        "100, boqol",
        "101, boqol iyo kow",
        "120, boqol iyo labaatan",
        "200, laba boqol",
        "350, saddex boqol iyo konton",
        "500, shan boqol",
        "999, sagaal boqol iyo sagaal iyo sagaashan",
        "1000, kun",
        "1001, kun iyo kow",
        "1100, kun iyo boqol",
        "2000, laba kun",
        "2500, laba kun iyo shan boqol",
        "3000, saddex kun",
        "5000, shan kun",
        "10000, toban kun",
        "25000, shan iyo labaatan kun",
        "50000, konton kun",
        "100000, boqol kun",
        "150000, boqol iyo konton kun",
        "999999, sagaal boqol iyo sagaal iyo sagaashan kun iyo sagaal boqol iyo sagaal iyo sagaashan",
        "1000000, hal malyuun",
        "1250000, hal malyuun iyo laba boqol iyo konton kun",
        "2000000, laba malyuun",
        "2000500, laba malyuun iyo shan boqol"
    )
    fun `whole numbers are joined ones first with iyo`(n: Long, expected: String) {
        assertEquals(expected, words(n))
    }

    @Test
    fun `zero is eber`() {
        assertEquals("eber", words(0))
    }

    @ParameterizedTest
    @ValueSource(longs = [-1, 1_000_000_000])
    fun `numbers outside 0 to 999 million are rejected`(n: Long) {
        assertNull(SomaliNumbers.wordsOrNull(n))
    }

    @ParameterizedTest(name = "{1} {0} = {2}")
    @CsvSource(
        "1, USD, hal doolar",
        "5, USD, shan doolar",
        "120, USD, boqol iyo labaatan doolar",
        "7.5, USD, toddoba doolar iyo konton senti",
        "12.5, USD, laba iyo toban doolar iyo konton senti",
        "45.25, USD, shan iyo afartan doolar iyo shan iyo labaatan senti",
        "60.4, USD, lixdan doolar iyo afartan senti",
        "3.05, USD, saddex doolar iyo shan senti",
        "2.01, USD, laba doolar iyo hal senti",
        "0.5, USD, konton senti",
        "8.00, USD, siddeed doolar",
        "8., USD, siddeed doolar",
        "500, SLSH, shan boqol shilin",
        "2000, SLSH, laba kun shilin",
        "1250000, SLSH, hal malyuun iyo laba boqol iyo konton kun shilin"
    )
    fun `amounts are spoken as currency plus cents`(
        amount: String,
        currency: Currency,
        expected: String
    ) {
        assertEquals(expected, SomaliNumbers.amount(amount, currency)?.joinToString(" "))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "abc", "1.234", "1,000", "-5", ".5x", "0", "0.00", "1000000000"])
    fun `amounts that cannot be spoken exactly give null`(amount: String) {
        assertNull(SomaliNumbers.amount(amount, Currency.USD))
    }

    @Test
    fun `phone digits are read one by one with kow for one`() {
        assertEquals("lix eber eber kow", SomaliNumbers.digits("6001").joinToString(" "))
        assertEquals("afar shan lix toddoba", SomaliNumbers.digits("4567").joinToString(" "))
    }

    @Test
    fun `non digits in a phone number are skipped`() {
        assertEquals("laba laba saddex", SomaliNumbers.digits("+2-23").joinToString(" "))
    }
}
