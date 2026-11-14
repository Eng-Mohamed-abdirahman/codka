package com.codka.app.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmsFingerprintTest {
    private val sms =
        "[-[--ZAAD SERVICES--]-] Tix: 16100000001, Waxaad \$120 ka heshay AXMED CALI (633000001)"

    @Test
    fun `the same SMS gives the same fingerprint`() {
        assertEquals(SmsFingerprint.of(sms), SmsFingerprint.of(sms))
    }

    @Test
    fun `spacing differences are ignored`() {
        assertEquals(
            SmsFingerprint.of(sms),
            SmsFingerprint.of("  " + sms.replace(" ", "\n  ") + " ")
        )
    }

    @Test
    fun `another transaction number gives another fingerprint`() {
        assertNotEquals(
            SmsFingerprint.of(sms),
            SmsFingerprint.of(sms.replace("16100000001", "16100000002"))
        )
    }

    @Test
    fun `the fingerprint does not contain the SMS text`() {
        val print = SmsFingerprint.of(sms)
        assertEquals(64, print.length)
        assertTrue(print.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
