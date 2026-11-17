package com.codka.app.sms

import com.codka.app.parser.Currency
import com.codka.app.parser.ParsedPayment
import com.codka.app.parser.Provider
import org.junit.Assert.assertEquals
import org.junit.Test

/** Task 13: the fake-SMS rules. */
class FakeCheckTest {
    private fun payment(
        provider: Provider = Provider.ZAAD,
        currency: Currency = Currency.USD,
        amount: String = "120",
        balance: String? = "150.0049"
    ) = ParsedPayment(provider, currency, amount, "AXMED CALI", "633000001", balance)

    private fun check(payment: ParsedPayment, sender: String) =
        FakeCheck.RULES.check(payment, sender)

    @Test
    fun eachAccountsOfficialSenderIsReal() {
        val senders =
            mapOf(
                (Provider.ZAAD to Currency.SLSH) to "222",
                (Provider.ZAAD to Currency.USD) to "898",
                (Provider.EDAHAB to Currency.SLSH) to "eDahab-SHL",
                (Provider.EDAHAB to Currency.USD) to "eDahab-USD"
            )
        senders.forEach { (account, sender) ->
            val (provider, currency) = account
            assertEquals(
                sender,
                FakeCheck.Verdict.Real,
                check(payment(provider, currency), sender)
            )
        }
    }

    @Test
    fun senderIgnoresCaseAndSpaces() {
        val edahab = payment(Provider.EDAHAB, Currency.USD)
        assertEquals(FakeCheck.Verdict.Real, check(edahab, " EDAHAB-USD "))
    }

    @Test
    fun aPhoneNumberSendingAPaymentSmsIsFake() {
        assertEquals(
            FakeCheck.Verdict.Fake(FakeCheck.WRONG_SENDER),
            check(payment(), "+252634000000")
        )
    }

    @Test
    fun theOtherCurrencysSenderIsFake() {
        // A dollar payment SMS must come from 898, not from the shilling sender 222.
        assertEquals(
            FakeCheck.Verdict.Fake(FakeCheck.WRONG_SENDER),
            check(payment(Provider.ZAAD, Currency.USD), "222")
        )
        assertEquals(
            FakeCheck.Verdict.Fake(FakeCheck.WRONG_SENDER),
            check(payment(Provider.EDAHAB, Currency.SLSH), "eDahab-USD")
        )
    }

    @Test
    fun aBalanceSmallerThanTheAmountIsFake() {
        assertEquals(
            FakeCheck.Verdict.Fake(FakeCheck.BALANCE_BELOW_AMOUNT),
            check(payment(amount = "120", balance = "119.9999"), "898")
        )
        assertEquals(
            FakeCheck.Verdict.Fake(FakeCheck.BALANCE_BELOW_AMOUNT),
            check(
                payment(Provider.EDAHAB, Currency.SLSH, amount = "25,000", balance = "9,250.8"),
                "eDahab-SHL"
            )
        )
    }

    @Test
    fun aBalanceEqualToTheAmountOrAboveIsReal() {
        assertEquals(
            FakeCheck.Verdict.Real,
            check(payment(amount = "120", balance = "120.0000"), "898")
        )
        assertEquals(
            FakeCheck.Verdict.Real,
            check(
                payment(Provider.EDAHAB, Currency.SLSH, amount = "25000", balance = "31,250.8"),
                "eDahab-SHL"
            )
        )
    }

    @Test
    fun noBalanceInTheSmsIsNotAReasonToDoubtIt() {
        assertEquals(FakeCheck.Verdict.Real, check(payment(balance = null), "898"))
    }
}
