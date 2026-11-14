package com.codka.app.voice

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Plays announcements one after another (task 18). Main thread only.
 *
 * Each announcement is a playlist of recorded clips, played by one
 * ExoPlayer so the clips follow each other with no gaps. It takes audio focus (music pauses)
 * and turns the media volume up to full while speaking, then puts it back.
 */
class Announcer(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audio = appContext.getSystemService(AudioManager::class.java)
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
        play(Announcement.build(request))
    }

    private fun play(clips: List<String>) {
        val items = clips.mapNotNull(::mediaItem)
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

        // Safety net: if playback stops moving (no audio focus during a phone call, say),
        // don't block the queue forever. Restarted at every clip, so long sentences are fine.
        fun watchdog() {
            main.removeCallbacksAndMessages(player)
            main.postAtTime({
                Log.w(TAG, "Announcement stuck, skipping it")
                end()
            }, player, SystemClock.uptimeMillis() + STUCK_MS)
        }
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) end()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = watchdog()

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Announcement failed", error)
                    end()
                }
            }
        )
        watchdog()
        Log.i(TAG, "Announcing ${items.size} clips")
        player.setMediaItems(items)
        player.prepare()
        player.play()
    }

    private fun mediaItem(clip: String): MediaItem? {
        val id = appContext.resources.getIdentifier(clip, "raw", appContext.packageName)
        if (id == 0) {
            Log.e(TAG, "Missing clip $clip")
            return null
        }
        return MediaItem.fromUri(Uri.parse("android.resource://${appContext.packageName}/$id"))
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

        /** No new clip for this long means playback is stuck (the longest clip is about 6 s). */
        const val STUCK_MS = 15_000L
    }
}
