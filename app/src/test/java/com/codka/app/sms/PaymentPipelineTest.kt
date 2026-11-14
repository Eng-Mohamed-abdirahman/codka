package com.codka.app.sms

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.codka.app.data.CodkaDatabase
import com.codka.app.data.PaymentStore
import com.codka.app.data.Settings
import com.codka.app.parser.Currency
import com.codka.app.parser.PaymentParser
import com.codka.app.parser.Provider
import com.codka.app.parser.SmsFormatLoader
import com.codka.app.voice.AnnounceRequest
import java.io.File
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

/** Task 19: real SMS samples -> real parser -> real database -> what gets spoken. */
@RunWith(RobolectricTestRunner::class)
class PaymentPipelineTest {
    private lateinit var db: CodkaDatabase
    private lateinit var store: PaymentStore
    private lateinit var settings: Settings
    private val spoken = mutableListOf<AnnounceRequest>()
    private val parser =
        PaymentParser(SmsFormatLoader.fromJson(File("src/main/assets/parsers.json").readText()))
    private val now = 1_794_636_000_000L

    private fun sample(name: String) =
        requireNotNull(javaClass.classLoader?.getResource("sms-samples/$name")) { name }.readText()

    private fun samples(folder: String) =
        File(requireNotNull(javaClass.classLoader?.getResource(folder)).toURI())
            .listFiles { f -> f.name.endsWith(".txt") }
            .orEmpty()
            .sortedBy { it.name }

    private fun pipeline(fakeCheck: FakeCheck = FakeCheck.NONE) =
        PaymentPipeline(parser, store, settings, fakeCheck) { spoken += it }

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
    fun everyPaymentSampleIsSavedAndSpoken() = runTest {
        val files = samples("sms-samples")
        assertEquals(20, files.size)
        files.forEachIndexed { i, file ->
            spoken.clear()
            val outcome = pipeline().handle(file.readText(), now + i)
            assertTrue("${file.name}: $outcome", outcome is PaymentPipeline.Outcome.Payment)

            val saved = db.payments().byId((outcome as PaymentPipeline.Outcome.Payment).id)!!
            val expected = parser.parse(file.readText())!!
            assertEquals(
                file.name,
                listOf(
                    AnnounceRequest.Payment(
                        expected.currency,
                        expected.amount,
                        expected.senderPhone
                    )
                ),
                spoken
            )
            assertEquals(file.name, expected.balance, saved.balanceAfter)
            assertEquals(
                file.name,
                expected.balance,
                settings.lastBalance(saved.provider, saved.currency)
            )
        }
        assertEquals(20, store.today(now).first().size)
    }

    @Test
    fun theSameSmsTwiceIsSpokenOnce() = runTest {
        val sms = sample("zaad-usd-001.txt")
        assertTrue(pipeline().handle(sms, now) is PaymentPipeline.Outcome.Payment)
        assertEquals(PaymentPipeline.Outcome.Duplicate, pipeline().handle(sms, now + 3_000))
        assertEquals(1, spoken.size)
        assertEquals(1, store.today(now).first().size)
    }

    @Test
    fun smsThatAreNotPaymentsAreIgnoredSilently() = runTest {
        val files = samples("sms-samples/not-payment")
        assertTrue(files.size >= 8)
        files.forEach { file ->
            assertEquals(
                file.name,
                PaymentPipeline.Outcome.Ignored,
                pipeline().handle(file.readText(), now)
            )
        }
        assertTrue(spoken.isEmpty())
        assertNull(store.last().first())
    }

    @Test
    fun anUnreadableMoneyReceivedSmsSaysUnknown() = runTest {
        // A new Zaad wording the parser doesn't know yet.
        val sms =
            "[-[--ZAAD SERVICES--]-] Tix: 16100000009, " +
                "Lacag dhan \$12 ayaad ka heshay XAWO (633000009)"
        assertEquals(PaymentPipeline.Outcome.Unknown, pipeline().handle(sms, now))
        assertEquals(listOf<AnnounceRequest>(AnnounceRequest.Unknown), spoken)
        assertNull(store.last().first())
    }

    @Test
    fun anAmountThatCannotBeSaidExactlySaysUnknown() = runTest {
        val sms = sample("zaad-usd-001.txt").replace("\$120", "\$1.234")
        assertEquals(PaymentPipeline.Outcome.Unknown, pipeline().handle(sms, now))
        assertEquals(listOf<AnnounceRequest>(AnnounceRequest.Unknown), spoken)
        assertNull(store.last().first())
    }

    @Test
    fun aFakePaymentIsSavedAsSuspiciousAndTheWarningIsSpoken() = runTest {
        settings.setLastBalance(Provider.ZAAD, Currency.USD, "30")
        val fakeCheck =
            FakeCheck { _, _, last -> FakeCheck.Verdict.Fake("balance did not go up from $last") }

        val outcome = pipeline(fakeCheck).handle(sample("zaad-usd-001.txt"), now)

        assertTrue(outcome is PaymentPipeline.Outcome.Fake)
        assertEquals(listOf<AnnounceRequest>(AnnounceRequest.Fake), spoken)
        val saved = db.payments().byId((outcome as PaymentPipeline.Outcome.Fake).id)!!
        assertTrue(saved.isSuspicious)
        assertEquals("balance did not go up from 30", saved.reason)
        // The made-up balance of a fake SMS must not replace the real one.
        assertEquals("30", settings.lastBalance(Provider.ZAAD, Currency.USD))
    }

    @Test
    fun moneyReceivedNeedsAZaadOrEdahabMarker() {
        assertTrue(
            PaymentPipeline.looksLikeMoneyReceived(
                "[eDahab Service-Dollar] 5 Dollar Ayaad ka Heshay"
            )
        )
        assertFalse(
            PaymentPipeline.looksLikeMoneyReceived(sample("not-payment/fake-no-marker.txt"))
        )
        assertFalse(
            PaymentPipeline.looksLikeMoneyReceived(sample("not-payment/zaad-slsh-sent-person.txt"))
        )
    }
}
