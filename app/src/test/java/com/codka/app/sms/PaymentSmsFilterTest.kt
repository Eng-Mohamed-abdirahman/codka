package com.codka.app.sms

import com.codka.app.sms.PaymentSmsFilter.Provider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PaymentSmsFilterTest {
    private fun sample(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("sms-samples/$name")) {
            "$name missing"
        }.readText()

    @ParameterizedTest
    @ValueSource(strings = ["001", "002", "003", "004", "005"])
    fun `every Zaad sample is a Zaad payment`(n: String) {
        listOf("zaad-usd-$n.txt", "zaad-slsh-$n.txt").forEach { file ->
            val sms = sample(file)
            assertTrue(PaymentSmsFilter.isPayment(sms), file)
            assertEquals(Provider.ZAAD, PaymentSmsFilter.providerOf(sms), file)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["001", "002", "003", "004", "005"])
    fun `every eDahab sample is an eDahab payment`(n: String) {
        listOf("edahab-usd-$n.txt", "edahab-slsh-$n.txt").forEach { file ->
            val sms = sample(file)
            assertTrue(PaymentSmsFilter.isPayment(sms), file)
            assertEquals(Provider.EDAHAB, PaymentSmsFilter.providerOf(sms), file)
        }
    }

    @Test
    fun `ordinary SMS is not a payment`() {
        assertFalse(PaymentSmsFilter.isPayment("Salaan, sidee tahay? Berri ayaan imanayaa."))
        assertFalse(PaymentSmsFilter.isPayment("Waxaad $50 ka heshay saaxiibkaa"))
    }
}
