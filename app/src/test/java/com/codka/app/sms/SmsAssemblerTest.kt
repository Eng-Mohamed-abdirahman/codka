package com.codka.app.sms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SmsAssemblerTest {
    @Test
    fun `joins the parts of a long SMS back into one message`() {
        val parts =
            listOf(
                IncomingSms("ZAAD", "[-[--ZAAD SERVICES--]-] Waxaad \$120 ka ", 1_000),
                IncomingSms("ZAAD", "heshay  AXMED CALI FAARAX (633000001)", 1_001)
            )
        val result = SmsAssembler.assemble(parts)
        assertEquals(1, result.size)
        assertEquals(
            "[-[--ZAAD SERVICES--]-] Waxaad \$120 ka heshay  AXMED CALI FAARAX (633000001)",
            result[0].body
        )
        assertEquals(1_000, result[0].sentAtMillis)
    }

    @Test
    fun `keeps messages from different senders apart`() {
        val parts =
            listOf(
                IncomingSms("ZAAD", "part one", 1),
                IncomingSms("252634000002", "hello", 2),
                IncomingSms("ZAAD", " part two", 3)
            )
        val result = SmsAssembler.assemble(parts).associateBy { it.sender }
        assertEquals("part one part two", result.getValue("ZAAD").body)
        assertEquals("hello", result.getValue("252634000002").body)
    }

    @Test
    fun `no parts gives no messages`() {
        assertEquals(emptyList<IncomingSms>(), SmsAssembler.assemble(emptyList()))
    }
}
