package com.codka.app.voice

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * Plays announcements one after another (task 18). Main thread only.
 *
 * Each announcement is a playlist of recorded clips (plus the TTS name file), played by one
 * ExoPlayer so the clips follow each other with no gaps. It takes audio focus (music pauses)
 * and turns the media volume up to full while speaking, then puts it back.
 */
class Announcer(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private val nameVoice = NameVoice(appContext)
    private val queue = ArrayDeque<AnnounceRequest>()
    private var busy = false

    /** Called on the main thread when the queue is empty again. */
    var onIdle: (() -> Unit)? = null

    fun announce(request: AnnounceRequest) {
        queue.addLast(request)
        if (!busy) next()
    }

    private fun next() {
        val request = queue.removeFirstOrNull()
        if (request == null) {
            busy = false
            onIdle?.invoke()
            return
        }
        busy = true
        if (request is AnnounceRequest.Payment && Announcement.wantsName(request)) {
            nameVoice.prepare(Announcement.displayName(request.senderName)!!) { nameFile ->
                play(Announcement.build(request, canSpeakName = nameFile != null), nameFile)
            }
        } else {
            play(Announcement.build(request, canSpeakName = false), null)
        }
    }

    private fun play(parts: List<Part>, nameFile: File?) {
        val items = parts.mapNotNull { part -> mediaItem(part, nameFile) }
        val player = ExoPlayer.Builder(appContext).build()
        player.setAudioAttributes(
            AudioAttributes
                .Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            // handleAudioFocus
            true
        )
        val restoreVolume = volumeToMax()
        var ended = false

        fun end() {
            if (ended) return
            ended = true
            Log.i(TAG, "Announcement done")
            main.removeCallbacksAndMessages(player)
            player.release()
            restoreVolume()
            next()
        }
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) end()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Announcement failed", error)
                    end()
                }
            }
        )
        // Safety net: if audio focus is never given (a phone call, say), don't block the queue forever.
        main.postAtTime({ end() }, player, android.os.SystemClock.uptimeMillis() + MAX_MS)
        Log.i(TAG, "Announcing ${items.size} clips, name=${nameFile != null}")
        player.setMediaItems(items)
        player.prepare()
        player.play()
    }

    private fun mediaItem(part: Part, nameFile: File?): MediaItem? = when (part) {
        is Part.Clip -> {
            val id = appContext.resources.getIdentifier(
                part.name,
                "raw",
                appContext.packageName
            )
            if (id == 0) {
                Log.e(TAG, "Missing clip ${part.name}")
                null
            } else {
                MediaItem.fromUri(Uri.parse("android.resource://${appContext.packageName}/$id"))
            }
        }
        is Part.Name -> nameFile?.let { MediaItem.fromUri(Uri.fromFile(it)) }
    }

    /** Turns the media volume to full and returns a function that puts it back. */
    private fun volumeToMax(): () -> Unit {
        val stream = AudioManager.STREAM_MUSIC
        val before: Int
        val max: Int
        try {
            before = audio.getStreamVolume(stream)
            max = audio.getStreamMaxVolume(stream)
            audio.setStreamVolume(stream, max, 0)
        } catch (e: SecurityException) {
            Log.w(TAG, "Can't change the volume", e)
            return {}
        }
        return {
            // Only restore if nobody changed the volume while Codka was speaking.
            runCatching {
                if (audio.getStreamVolume(stream) == max) audio.setStreamVolume(stream, before, 0)
            }
        }
    }

    private companion object {
        const val TAG = "Codka"
        const val MAX_MS = 60_000L
    }
}
