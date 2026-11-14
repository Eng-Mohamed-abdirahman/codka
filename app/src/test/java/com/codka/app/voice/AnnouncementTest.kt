package com.codka.app.voice

import com.codka.app.parser.Currency
import com.codka.app.parser.PaymentParser
import com.codka.app.parser.SmsFormatLoader
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class AnnouncementTest {
    // Unit tests run from the app/ folder.
    private val rawDir = File("src/main/res/raw")

    private fun payment(
        amount: String,
        currency: Currency = Currency.USD,
        phone: String = "633000001"
    ) = AnnounceRequest.Payment(currency, amount, phone)

    private fun text(clips: List<String>) = clips.joinToString(" ")

    private val from633000001 =
        "p_ka_timid_lambarka n_6_lix n_3_saddex n_3_saddex n_0_eber n_0_eber n_0_eber n_0_eber n_0_eber n_1_kow"

    @Test
    fun `payment says amount, currency and the full number`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_5_shan w_iyo n_20_labaatan c_doolar $from633000001",
            text(Announcement.once(payment("25")))
        )
    }

    @Test
    fun `cents are spoken as doolar iyo senti`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_7_toddoba c_doolar w_iyo n_50_konton c_senti $from633000001",
            text(Announcement.once(payment("7.5")))
        )
    }

    @Test
    fun `shilin amounts use shilin`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_2_laba n_1000_kun c_shilin $from633000001",
            text(Announcement.once(payment("2000", Currency.SLSH)))
        )
    }

    @Test
    fun `the 252 country code is not read`() {
        assertEquals("654000004", Announcement.localNumber("+252 654 000 004"))
        assertEquals("654000004", Announcement.localNumber("252654000004"))
        assertEquals("654000004", Announcement.localNumber("(654000004)"))
        // A 9-digit local number that happens to start with 252 is kept whole.
        assertEquals("252000004", Announcement.localNumber("252000004"))
    }

    @Test
    fun `no usable number still says the amount`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_5_shan c_doolar",
            text(Announcement.once(payment("5", phone = "")))
        )
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_5_shan c_doolar",
            text(Announcement.once(payment("5", phone = "12")))
        )
    }

    @Test
    fun `an amount that can't be said exactly becomes unknown message`() {
        assertEquals("p_fariin_aan_la_garanayn", text(Announcement.once(payment("1.234"))))
    }

    @Test
    fun `fake and unknown have their own sentences`() {
        assertEquals("p_digniin_been_abuur", text(Announcement.once(AnnounceRequest.Fake)))
        assertEquals("p_fariin_aan_la_garanayn", text(Announcement.once(AnnounceRequest.Unknown)))
    }

    @Test
    fun `everything is said twice with mar kale in between`() {
        val once = Announcement.once(payment("120"))
        assertEquals(once + "p_mar_kale" + once, Announcement.build(payment("120")))
        assertEquals(
            "p_digniin_been_abuur p_mar_kale p_digniin_been_abuur",
            text(Announcement.build(AnnounceRequest.Fake))
        )
    }

    @Test
    fun `every clip the announcer can use exists in res raw`() {
        val sentences =
            listOf(
                Announcement.ARRIVED,
                Announcement.FROM_NUMBER,
                Announcement.ONCE_MORE,
                Announcement.FAKE,
                Announcement.UNKNOWN,
                Announcement.TEST
            )
        val missing = (VoiceClips.all + sentences).filterNot { File(rawDir, "$it.ogg").isFile }
        assertEquals(emptyList<String>(), missing, "missing clips in $rawDir")
    }

    /** Real SMS -> parser -> announcement, so the real number format is checked too. */
    @TestFactory
    fun `every sample SMS is announced with its full number`(): List<DynamicTest> {
        val parser =
            PaymentParser(SmsFormatLoader.fromJson(File("src/main/assets/parsers.json").readText()))
        val samples =
            File(requireNotNull(javaClass.classLoader?.getResource("sms-samples")).toURI())
        val files = samples.listFiles { f ->
            f.name.endsWith(".txt")
        }.orEmpty().sortedBy { it.name }
        return files.map { file ->
            DynamicTest.dynamicTest(file.name) {
                val sms =
                    requireNotNull(parser.parse(file.readText())) { "${file.name} not parsed" }
                val clips = Announcement.once(
                    AnnounceRequest.Payment(sms.currency, sms.amount, sms.senderPhone)
                )
                val info = "${file.name} -> ${text(clips)}"
                val number = Announcement.localNumber(sms.senderPhone)
                assertEquals(9, number.length, info)
                assertEquals(Announcement.ARRIVED, clips.first(), info)
                // The number is the end: FROM_NUMBER, then one clip per digit.
                assertEquals(Announcement.FROM_NUMBER, clips[clips.size - number.length - 1], info)
                assertEquals(
                    SomaliNumbers.digits(number).map(VoiceClips::forWord),
                    clips.takeLast(number.length),
                    info
                )
            }
        }
    }
}
