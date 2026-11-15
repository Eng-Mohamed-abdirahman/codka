package com.codka.app.ui.setup

import com.codka.app.ui.setup.SetupFlow.State
import com.codka.app.ui.setup.SetupFlow.Step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetupFlowTest {
    @Test
    fun `App Launch is a step only on Tecno, Infinix and itel`() {
        assertEquals(
            listOf(Step.SMS, Step.BATTERY, Step.AUTO_START, Step.VOICE),
            SetupFlow.steps(true)
        )
        assertEquals(listOf(Step.SMS, Step.BATTERY, Step.VOICE), SetupFlow.steps(false))
    }

    @Test
    fun `SMS permission must be given before going on`() {
        assertFalse(SetupFlow.canContinue(Step.SMS, State()))
        assertTrue(SetupFlow.canContinue(Step.SMS, State(smsAllowed = true)))
    }

    @Test
    fun `the voice must be heard before finishing`() {
        assertFalse(SetupFlow.canContinue(Step.VOICE, State(smsAllowed = true)))
        assertTrue(SetupFlow.canContinue(Step.VOICE, State(voiceHeard = true)))
    }

    @Test
    fun `battery and App Launch can be skipped but are not marked done`() {
        assertTrue(SetupFlow.canContinue(Step.BATTERY, State()))
        assertFalse(SetupFlow.isDone(Step.BATTERY, State()))
        assertTrue(SetupFlow.isDone(Step.BATTERY, State(batteryUnrestricted = true)))

        assertTrue(SetupFlow.canContinue(Step.AUTO_START, State()))
        assertFalse(SetupFlow.isDone(Step.AUTO_START, State()))
        assertTrue(SetupFlow.isDone(Step.AUTO_START, State(autoStartConfirmed = true)))
    }
}
