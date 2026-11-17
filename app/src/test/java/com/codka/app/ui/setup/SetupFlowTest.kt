package com.codka.app.ui.setup

import com.codka.app.ui.setup.SetupFlow.State
import com.codka.app.ui.setup.SetupFlow.Step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetupFlowTest {
    @Test
    fun `four steps in the design's order`() {
        assertEquals(listOf(Step.WELCOME, Step.SMS, Step.BATTERY, Step.VOICE), SetupFlow.steps)
    }

    @Test
    fun `welcome can always go on`() {
        assertTrue(SetupFlow.canContinue(Step.WELCOME, State()))
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
    fun `battery can be passed but is only done when unrestricted`() {
        assertTrue(SetupFlow.canContinue(Step.BATTERY, State()))
        assertFalse(SetupFlow.isDone(Step.BATTERY, State()))
        assertTrue(SetupFlow.isDone(Step.BATTERY, State(batteryUnrestricted = true)))
    }
}
