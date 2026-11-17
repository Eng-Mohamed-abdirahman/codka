package com.codka.app.ui.setup

/**
 * Task 23: the one-time setup a helper does on the seller's phone (design boards 12, 16, 17),
 * as plain logic so it can be unit tested. The screen ([SetupScreen]) only draws the current step.
 */
object SetupFlow {
    enum class Step {
        /** Ku soo dhawow: what Codka does, and that messages stay on the phone. */
        WELCOME,

        /** Let Codka receive SMS. Without it nothing works, so it can't be skipped. */
        SMS,

        /**
         * Let Codka run in the background ("no restrictions"). On Tecno / Infinix / itel the same
         * step also points to Phone Master's auto-start setting.
         */
        BATTERY,

        /** Play a test payment and check it can be heard. */
        VOICE
    }

    /** What the helper has done so far. */
    data class State(
        val smsAllowed: Boolean = false,
        val batteryUnrestricted: Boolean = false,
        val voiceHeard: Boolean = false
    )

    val steps: List<Step> = Step.entries

    fun isDone(step: Step, state: State): Boolean = when (step) {
        Step.WELCOME -> true
        Step.SMS -> state.smsAllowed
        Step.BATTERY -> state.batteryUnrestricted
        Step.VOICE -> state.voiceHeard
    }

    /**
     * Whether the helper may go on. SMS and the voice test must really be done. The battery step
     * can be passed with "Waan sameeyay" (I did it), because on some phones Android's screens
     * don't report back.
     */
    fun canContinue(step: Step, state: State): Boolean = when (step) {
        Step.WELCOME, Step.BATTERY -> true
        Step.SMS, Step.VOICE -> isDone(step, state)
    }
}
