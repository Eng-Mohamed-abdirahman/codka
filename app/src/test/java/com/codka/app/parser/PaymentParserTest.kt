package com.codka.app.parser

import java.io.File
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class PaymentParserTest {
    // Unit tests run from the app/ folder, so the real asset file is used.
    private val parser =
        PaymentParser(SmsFormatLoader.fromJson(File("src/main/assets/parsers.json").readText()))

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("sms-samples/$name")) {
            "$name missing"
        }.readText()

    /** Money sent, top-ups, exchange, ordinary SMS: Codka must stay silent for all of them. */
    @TestFactory
    fun `ignores every not-payment sample`(): List<DynamicTest> {
        val folder =
            File(
                requireNotNull(
                    javaClass.classLoader?.getResource("sms-samples/not-payment")
                ).toURI()
            )
        val files = folder.listFiles { f -> f.name.endsWith(".txt") }.orEmpty().sortedBy { it.name }
        assertTrue(files.size >= 8, "not-payment samples missing")
        return files.map { file ->
            DynamicTest.dynamicTest(file.name) {
                assertNull(parser.parse(file.readText()), "${file.name} must not be a payment")
            }
        }
    }

    @TestFactory
    fun `parses every sample exactly as expected json says`(): List<DynamicTest> {
        val samples = JSONObject(resource("expected.json")).getJSONArray("samples")
        return (0 until samples.length()).map { i ->
            val expected = samples.getJSONObject(i)
            val file = expected.getString("file")
            DynamicTest.dynamicTest(file) {
                val parsed = parser.parse(resource(file))
                assertNotNull(parsed, "$file was not recognised")
                assertEquals(
                    Provider.valueOf(expected.getString("provider")),
                    parsed!!.provider,
                    "$file provider"
                )
                assertEquals(
                    Currency.valueOf(expected.getString("currency")),
                    parsed.currency,
                    "$file currency"
                )
                assertEquals(expected.getString("amount"), parsed.amount, "$file amount")
                assertEquals(
                    expected.getString("senderName"),
                    parsed.senderName,
                    "$file sender name"
                )
            }
        }
    }

    @Test
    fun `keeps decimals exactly, never rounds`() {
        assertEquals("12.5", parser.parse(resource("zaad-usd-004.txt"))!!.amount)
        assertEquals("45.25", parser.parse(resource("zaad-usd-005.txt"))!!.amount)
        assertEquals("7.5", parser.parse(resource("edahab-usd-004.txt"))!!.amount)
        assertEquals("60.4", parser.parse(resource("edahab-usd-005.txt"))!!.amount)
    }

    @Test
    fun `removes only the thousands commas`() {
        assertEquals("1250000", parser.parse(resource("zaad-slsh-005.txt"))!!.amount)
        assertEquals("100000", parser.parse(resource("edahab-slsh-003.txt"))!!.amount)
    }

    @Test
    fun `reads the sender phone for the last-4-digits fallback`() {
        assertEquals("633000001", parser.parse(resource("zaad-usd-001.txt"))!!.senderPhone)
        assertEquals("654000004", parser.parse(resource("edahab-slsh-001.txt"))!!.senderPhone)
    }

    @Test
    fun `eDahab still parses when the line breaks are lost`() {
        val oneLine = resource("edahab-usd-001.txt").replace("\n", " ")
        val parsed = parser.parse(oneLine)
        assertEquals("100", parsed!!.amount)
        assertEquals("MAXAMED CUMAR WARSAME", parsed.senderName)
    }

    @Test
    fun `ordinary and broken messages are not payments`() {
        assertNull(parser.parse("Salaan, sidee tahay? Berri ayaan imanayaa."))
        // no Zaad header
        assertNull(parser.parse("Waxaad \$50 ka heshay  AXMED CALI (633000001)"))
        // Zaad header, but no payment
        assertNull(parser.parse("[-[--ZAAD SERVICES--]-] Lacagta waa la diray."))
        assertNull(parser.parse(""))
    }

    @Test
    fun `parsers json has all four formats`() {
        val ids = SmsFormatLoader.fromJson(File("src/main/assets/parsers.json").readText()).map {
            it.id
        }
        assertEquals(listOf("zaad-usd", "zaad-slsh", "edahab-usd", "edahab-slsh"), ids)
    }
}
