package com.codka.app.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Checks the SMS sample set itself, so the parser tests (coming next) can trust it:
 * every sample listed in expected.json exists and contains its sender name and amount.
 */
class SmsSamplesTest {
    private data class Sample(val file: String, val amount: String, val senderName: String)

    private fun resource(name: String): String? = javaClass.classLoader
        ?.getResource("sms-samples/$name")
        ?.readText()

    private fun samples(): List<Sample> {
        val json = requireNotNull(resource("expected.json")) { "expected.json is missing" }
        val entry =
            Regex(
                """\{\s*"file":\s*"([^"]+)".*?"amount":\s*"([^"]+)".*?"senderName":\s*"([^"]+)"\s*}"""
            )
        return entry.findAll(json).map {
            Sample(it.groupValues[1], it.groupValues[2], it.groupValues[3])
        }.toList()
    }

    @Test
    fun `there are 20 samples, 5 per provider and currency`() {
        val files = samples().map { it.file }
        assertEquals(20, files.size)
        listOf("zaad-usd", "zaad-slsh", "edahab-usd", "edahab-slsh").forEach { type ->
            assertEquals(5, files.count { it.startsWith("$type-") }, "samples for $type")
        }
    }

    @Test
    fun `every sample file exists and contains its sender name and amount`() {
        samples().forEach { sample ->
            val sms = requireNotNull(resource(sample.file)) { "${sample.file} is missing" }
            assertTrue(
                sms.contains(sample.senderName),
                "${sample.file}: name '${sample.senderName}' not found"
            )
            val amountInSms = Regex("""[\d,]+(\.\d+)?""").findAll(sms).map {
                it.value.replace(",", "")
            }
            assertTrue(
                sample.amount in amountInSms.toList(),
                "${sample.file}: amount ${sample.amount} not found"
            )
        }
    }
}
