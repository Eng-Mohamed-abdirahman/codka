package com.codka.app.ui.setup

/**
 * Task 23: the one-time setup a helper does on the seller's phone, as plain logic so it can be
 * unit tested. The screen ([SetupScreen]) only draws the current step.
 */
object SetupFlow {
    enum class Step {
        /** Let Codka receive SMS. Without it nothing works, so it can't be skipped. */
        SMS,

        /** Let Codka run in the background ("no battery restrictions"). */
        BATTERY,

        /** Tecno / Infinix / itel only: Phone Master > App Launch must allow Codka. */
        AUTO_START,

        /** Play a test payment and check it can be heard. */
        VOICE
    }

    /** What the helper has done so far. */
    data class State(
        val smsAllowed: Boolean = false,
        val batteryUnrestricted: Boolean = false,
        /** Android can't tell whether App Launch is on, so the helper confirms it. */
        val autoStartConfirmed: Boolean = false,
        val voiceHeard: Boolean = false
    )

    /** The steps for this phone: App Launch only exists on Transsion phones. */
    fun steps(isTranssion: Boolean): List<Step> =
        Step.entries.filter { it != Step.AUTO_START || isTranssion }

    fun isDone(step: Step, state: State): Boolean = when (step) {
        Step.SMS -> state.smsAllowed
        Step.BATTERY -> state.batteryUnrestricted
        Step.AUTO_START -> state.autoStartConfirmed
        Step.VOICE -> state.voiceHeard
    }

    /**
     * Whether "next" works yet. SMS and the voice test must really be done. Battery and
     * App Launch can be skipped, because on some phones Android's screens don't report back,
     * but the screen asks twice by not marking them done.
     */
    fun canContinue(step: Step, state: State): Boolean = when (step) {
        Step.SMS, Step.VOICE -> isDone(step, state)
        Step.BATTERY, Step.AUTO_START -> true
    }
}
