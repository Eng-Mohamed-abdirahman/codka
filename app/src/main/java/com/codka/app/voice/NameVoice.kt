package com.codka.app.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.File
import java.util.Locale

/**
 * Speaks the sender's name with the phone's text-to-speech voice, by writing it to a short
 * audio file that the announcer plays between the recorded clips (so there is no gap).
 *
 * Uses a **Somali** voice if the phone has one. Most phones don't, so otherwise a **Swahili**
 * voice reads the name, respelled by [NameSpelling] (Axmed Cali -> Ahmed Ali). An English
 * voice is never used: it reads Somali names badly. With neither voice, [prepare] gives null
 * and the announcer reads the last 4 digits of the number instead.
 */
class NameVoice(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    /**
     * Writes [name] to a WAV file and calls [done] on the main thread with it,
     * or with null if there is no usable voice or it took longer than [TIMEOUT_MS].
     */
    fun prepare(name: String, done: (File?) -> Unit) {
        var finished = false
        var tts: TextToSpeech? = null
        var timeout: Runnable? = null

        fun finish(file: File?) {
            main.post {
                if (finished) return@post
                finished = true
                timeout?.let(main::removeCallbacks)
                tts?.shutdown()
                done(file)
            }
        }
        timeout =
            Runnable {
                Log.w(TAG, "Name voice timed out")
                finish(null)
            }
        main.postDelayed(timeout, TIMEOUT_MS)

        tts =
            TextToSpeech(appContext) { status ->
                val engine = tts
                if (status != TextToSpeech.SUCCESS || engine == null) {
                    return@TextToSpeech finish(null)
                }
                val text = pickVoice(engine, name) ?: return@TextToSpeech finish(null)
                val out = File(appContext.cacheDir, "name.wav").apply { delete() }
                engine.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) =
                            finish(out.takeIf { it.length() > 0 })

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) = finish(null)

                        override fun onError(utteranceId: String?, errorCode: Int) = finish(null)
                    }
                )
                if (engine.synthesizeToFile(text, null, out, UTTERANCE) != TextToSpeech.SUCCESS) {
                    finish(null)
                }
            }
    }

    /** Sets the voice and returns the text to say, or null if the phone has no usable voice. */
    private fun pickVoice(engine: TextToSpeech, name: String): String? {
        if (engine.isLanguageAvailable(SOMALI) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = SOMALI
            return name
        }
        if (engine.isLanguageAvailable(SWAHILI) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = SWAHILI
            val spelled = NameSpelling.forSwahili(name)
            Log.i(TAG, "Name voice: Swahili, \"$name\" -> \"$spelled\"")
            return spelled
        }
        Log.i(TAG, "No Somali or Swahili TTS voice: reading the number instead")
        return null
    }

    private companion object {
        const val TAG = "Codka"
        const val UTTERANCE = "codka-name"
        const val TIMEOUT_MS = 10_000L
        val SOMALI: Locale = Locale.forLanguageTag("so")
        val SWAHILI: Locale = Locale.forLanguageTag("sw")
    }
}
