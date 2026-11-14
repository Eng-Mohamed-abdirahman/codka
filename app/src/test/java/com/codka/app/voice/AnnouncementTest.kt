package com.codka.app.voice

import com.codka.app.parser.Currency
import java.io.File
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class AnnouncementTest {
    // Unit tests run from the app/ folder.
    private val rawDir = File("src/main/res/raw")

    private fun payment(
        amount: String,
        currency: Currency = Currency.USD,
        name: String = "AXMED CALI FAARAX",
        phone: String = "252634561234"
    ) = AnnounceRequest.Payment(currency, amount, name, phone)

    /** Readable form: clips by name, TTS parts in [brackets]. */
    private fun text(parts: List<Part>) = parts.joinToString(" ") {
        when (it) {
            is Part.Clip -> it.name
            is Part.Name -> "[${it.text}]"
        }
    }

    @Test
    fun `payment with a name says amount, currency and the name`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_5_shan w_iyo n_20_labaatan c_doolar p_waxaa_soo_diray [Axmed Cali Faarax]",
            text(Announcement.once(payment("25"), canSpeakName = true))
        )
    }

    @Test
    fun `cents are spoken as doolar iyo senti`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_7_toddoba c_doolar w_iyo n_50_konton c_senti p_waxaa_soo_diray [Axmed Cali Faarax]",
            text(Announcement.once(payment("7.5"), canSpeakName = true))
        )
    }

    @Test
    fun `without a voice for names the last 4 digits are read`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_2_laba n_1000_kun c_shilin p_ka_timid_lambarka n_1_kow n_2_laba n_3_saddex n_4_afar",
            text(Announcement.once(payment("2000", Currency.SLSH), canSpeakName = false))
        )
    }

    @Test
    fun `an SMS with no name falls back to the last 4 digits`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_1_hal c_doolar p_ka_timid_lambarka n_6_lix n_0_eber n_0_eber n_1_kow",
            text(
                Announcement.once(
                    payment("1", name = "  ", phone = "+252 63 456 6001"),
                    canSpeakName = true
                )
            )
        )
    }

    @Test
    fun `no name and no usable number still says the amount`() {
        assertEquals(
            "p_lacag_ayaa_kuu_timid n_5_shan c_doolar",
            text(Announcement.once(payment("5", name = "", phone = "12"), canSpeakName = true))
        )
    }

    @Test
    fun `an amount that can't be said exactly becomes unknown message`() {
        assertEquals(
            "p_fariin_aan_la_garanayn",
            text(Announcement.once(payment("1.234"), canSpeakName = true))
        )
    }

    @Test
    fun `fake and unknown have their own sentences`() {
        assertEquals("p_digniin_been_abuur", text(Announcement.once(AnnounceRequest.Fake, true)))
        assertEquals(
            "p_fariin_aan_la_garanayn",
            text(Announcement.once(AnnounceRequest.Unknown, true))
        )
    }

    @Test
    fun `everything is said twice with mar kale in between`() {
        val once = Announcement.once(payment("120"), canSpeakName = true)
        assertEquals(
            once + Part.Clip("p_mar_kale") + once,
            Announcement.build(payment("120"), canSpeakName = true)
        )
        assertEquals(
            "p_digniin_been_abuur p_mar_kale p_digniin_been_abuur",
            text(Announcement.build(AnnounceRequest.Fake, canSpeakName = false))
        )
    }

    @Test
    fun `names are turned into normal case`() {
        assertEquals("Axmed Cali Faarax", Announcement.displayName("AXMED  CALI FAARAX "))
        assertEquals("Hodan", Announcement.displayName("hodan"))
        assertNull(Announcement.displayName("   "))
        assertNull(Announcement.displayName("-- 123"))
    }

    @Test
    fun `wantsName is true only for payments with a name`() {
        assertTrue(Announcement.wantsName(payment("5")))
        assertFalse(Announcement.wantsName(payment("5", name = "")))
        assertFalse(Announcement.wantsName(AnnounceRequest.Fake))
    }

    @Test
    fun `every clip the announcer can use exists in res raw`() {
        val sentences =
            listOf(
                Announcement.ARRIVED,
                Announcement.SENT_BY,
                Announcement.FROM_NUMBER,
                Announcement.ONCE_MORE,
                Announcement.FAKE,
                Announcement.UNKNOWN,
                Announcement.TEST
            )
        val missing = (VoiceClips.all + sentences).filterNot { File(rawDir, "$it.ogg").isFile }
        assertEquals(emptyList<String>(), missing, "missing clips in $rawDir")
    }

    @TestFactory
    fun `every sample SMS amount can be announced`(): List<DynamicTest> {
        val json = requireNotNull(
            javaClass.classLoader?.getResource("sms-samples/expected.json")
        ).readText()
        val samples = JSONObject(json).getJSONArray("samples")
        return (0 until samples.length()).map { i ->
            val s = samples.getJSONObject(i)
            DynamicTest.dynamicTest(s.getString("file")) {
                val parts =
                    Announcement.once(
                        payment(
                            s.getString("amount"),
                            Currency.valueOf(s.getString("currency")),
                            s.getString("senderName")
                        ),
                        canSpeakName = true
                    )
                assertEquals(
                    Part.Clip(Announcement.ARRIVED),
                    parts.first(),
                    "${s.getString("file")} -> ${text(parts)}"
                )
            }
        }
    }
}
