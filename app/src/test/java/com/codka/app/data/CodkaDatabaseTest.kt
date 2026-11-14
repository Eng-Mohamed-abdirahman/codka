package com.codka.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.codka.app.parser.Currency
import com.codka.app.parser.ParsedPayment
import com.codka.app.parser.Provider
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs the real Room database on the computer with Robolectric (JUnit 4). */
@RunWith(RobolectricTestRunner::class)
class CodkaDatabaseTest {
    private lateinit var db: CodkaDatabase
    private lateinit var store: PaymentStore
    private lateinit var settings: Settings

    // East Africa Time (Somaliland), no daylight saving.
    private val eat = TimeZone.getTimeZone("Africa/Mogadishu")

    // 14 Nov 2026 09:00 and 23:59, and 15 Nov 2026 00:00 in Somaliland.
    private val morning = 1_794_636_000_000L
    private val lateEvening = morning + (14 * 60 + 59) * 60_000L
    private val nextMidnight = morning + 15 * 3_600_000L
    private val yesterday = morning - 24 * 3_600_000L

    private fun payment(amount: String = "45.25", currency: Currency = Currency.USD) =
        ParsedPayment(Provider.ZAAD, currency, amount, "AXMED CALI", "633000001")

    private fun sms(tix: Int) =
        "[-[--ZAAD SERVICES--]-] Tix: $tix, Waxaad \$45.25 ka heshay AXMED CALI (633000001)"

    @Before
    fun open() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, CodkaDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        store = PaymentStore(db.payments())
        settings = Settings(db.settings())
    }

    @After
    fun close() = db.close()

    @Test
    fun savesAPaymentWithExactMoney() = runTest {
        val result = store.save(payment("45.25"), sms(1), morning)
        assertTrue(result is PaymentStore.SaveResult.Saved)

        val saved = db.payments().byId((result as PaymentStore.SaveResult.Saved).id)!!
        assertEquals(4525L, saved.amountMinor)
        assertEquals("45.25", saved.amount)
        assertEquals(Provider.ZAAD, saved.provider)
        assertEquals(Currency.USD, saved.currency)
        assertEquals("AXMED CALI", saved.senderName)
        assertEquals("633000001", saved.senderPhone)
        assertEquals(morning, saved.receivedAt)
        assertFalse(saved.isSuspicious)
        assertNull(saved.reason)
        assertNull(saved.balanceAfterMinor)
    }

    @Test
    fun theSameSmsIsNotSavedTwice() = runTest {
        assertTrue(store.save(payment(), sms(1), morning) is PaymentStore.SaveResult.Saved)
        assertEquals(
            PaymentStore.SaveResult.Duplicate,
            store.save(
                payment(),
                sms(1),
                morning + 5_000
            )
        )
        // Same amount and sender, but a new transaction: that is a real second payment.
        assertTrue(store.save(payment(), sms(2), morning + 9_000) is PaymentStore.SaveResult.Saved)
        assertEquals(2, store.today(morning, eat).first().size)
    }

    @Test
    fun anAmountThatIsNotExactIsNotSaved() = runTest {
        assertEquals(
            PaymentStore.SaveResult.BadAmount,
            store.save(payment("1.234"), sms(1), morning)
        )
        assertNull(store.last().first())
    }

    @Test
    fun lastIsTheNewestPayment() = runTest {
        assertNull(store.last().first())
        store.save(payment("5"), sms(1), morning)
        store.save(payment("7"), sms(2), lateEvening)
        store.save(payment("6"), sms(3), morning + 60_000)
        assertEquals("7", store.last().first()!!.amount)
    }

    @Test
    fun todayIsFromMidnightToMidnight() = runTest {
        store.save(payment("1"), sms(1), yesterday)
        store.save(payment("2"), sms(2), morning)
        store.save(payment("3"), sms(3), lateEvening)
        store.save(payment("4"), sms(4), nextMidnight)

        val today = store.today(morning, eat).first()
        assertEquals(listOf("3", "2"), today.map { it.amount })
    }

    @Test
    fun suspiciousPaymentsKeepTheirReason() = runTest {
        val id = (
            store.save(
                payment(),
                sms(1),
                morning,
                isSuspicious = true,
                reason = "balance"
            ) as PaymentStore.SaveResult.Saved
            ).id
        val saved = db.payments().byId(id)!!
        assertTrue(saved.isSuspicious)
        assertEquals("balance", saved.reason)
    }

    @Test
    fun settingsHaveDefaultsAndAreSaved() = runTest {
        assertEquals(100, settings.volume())
        assertFalse(settings.setupDone())
        assertNull(settings.lastBalance(Provider.ZAAD, Currency.USD))

        settings.setVolume(70)
        settings.setSetupDone(true)
        settings.setLastBalance(Provider.ZAAD, Currency.USD, 15_000)
        settings.setLastBalance(Provider.ZAAD, Currency.SLSH, 3_125_080)

        assertEquals(70, settings.volume())
        assertTrue(settings.setupDone())
        assertTrue(settings.watchSetupDone().first())
        assertEquals(15_000L, settings.lastBalance(Provider.ZAAD, Currency.USD))
        assertEquals(3_125_080L, settings.lastBalance(Provider.ZAAD, Currency.SLSH))
        assertNull(settings.lastBalance(Provider.EDAHAB, Currency.USD))
    }

    @Test
    fun volumeStaysBetween0And100() = runTest {
        settings.setVolume(150)
        assertEquals(100, settings.volume())
        settings.setVolume(-3)
        assertEquals(0, settings.volume())
    }
}
