package com.codka.app.sms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BackgroundSetupTest {
    @ParameterizedTest
    @ValueSource(strings = ["INFINIX", "Infinix", "TECNO", "itel", " infinix "])
    fun `Transsion phones get the App Launch step`(manufacturer: String) {
        assertTrue(BackgroundSetup.isTranssion(manufacturer))
    }

    @ParameterizedTest
    @ValueSource(strings = ["samsung", "Xiaomi", "Google", ""])
    fun `other phones don't`(manufacturer: String) {
        assertFalse(BackgroundSetup.isTranssion(manufacturer))
    }

    @Test
    fun `App Launch screen is the one found on the Infinix X6886`() {
        assertEquals(
            "com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity",
            BackgroundSetup.TRANSSION_AUTO_START_SCREENS.first()
        )
    }
}
