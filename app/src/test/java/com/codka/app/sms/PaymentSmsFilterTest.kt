package com.codka.app.sms

import com.codka.app.sms.PaymentSmsFilter.Provider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `every Zaad sample comes from Zaad`(n: String) {
        listOf("zaad-usd-$n.txt", "zaad-slsh-$n.txt").forEach { file ->
            assertEquals(Provider.ZAAD, PaymentSmsFilter.providerOf(sample(file)), file)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["001", "002", "003", "004", "005"])
    fun `every eDahab sample comes from eDahab`(n: String) {
        listOf("edahab-usd-$n.txt", "edahab-slsh-$n.txt").forEach { file ->
            assertEquals(Provider.EDAHAB, PaymentSmsFilter.providerOf(sample(file)), file)
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "zaad-slsh-sent-person.txt", "zaad-slsh-topup.txt",
            "zaad-exchange-slsh.txt", "zaad-exchange-usd.txt"
        ]
    )
    fun `Zaad non-payments still count as Zaad, so debug builds log them`(file: String) {
        assertEquals(Provider.ZAAD, PaymentSmsFilter.providerOf(sample("not-payment/$file")))
    }

    @Test
    fun `ordinary SMS is from neither`() {
        assertNull(PaymentSmsFilter.providerOf(sample("not-payment/ordinary.txt")))
    }
}
