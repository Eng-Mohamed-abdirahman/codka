package com.codka.app.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.codka.app.R
import com.codka.app.parser.Currency

/**
 * A short foreground service that keeps Codka alive while it speaks, so Android doesn't cut
 * the voice when the app is closed or the screen is locked. It stops as soon as the queue is empty.
 *
 * Use [AnnouncerService.announce] from anywhere (SMS receiver, test button).
 */
class AnnouncerService : Service() {
    private lateinit var announcer: Announcer

    override fun onCreate() {
        super.onCreate()
        announcer = Announcer(this).apply { onIdle = { stopSelf() } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        val request = intent?.let(::readRequest)
        if (request == null) {
            stopSelf()
        } else {
            announcer.announce(request)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun goForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    getString(R.string.announce_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_speaker)
                .setContentTitle(getString(R.string.announce_notification))
                .setOngoing(true)
                .setSilent(true)
                .build()
        val type =
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val TAG = "Codka"
        private const val CHANNEL = "announce"
        private const val NOTIFICATION_ID = 18
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_CURRENCY = "currency"
        private const val EXTRA_AMOUNT = "amount"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_PHONE = "phone"

        /** Used when Android refuses to start the service: speak anyway, without its protection. */
        private var fallback: Announcer? = null

        /** Speaks [request] (twice). Safe to call from the main thread of any component. */
        fun announce(context: Context, request: AnnounceRequest) {
            val intent = Intent(context, AnnouncerService::class.java)
            when (request) {
                AnnounceRequest.Fake -> intent.putExtra(EXTRA_KIND, "fake")
                AnnounceRequest.Unknown -> intent.putExtra(EXTRA_KIND, "unknown")
                is AnnounceRequest.Payment ->
                    intent
                        .putExtra(EXTRA_KIND, "payment")
                        .putExtra(EXTRA_CURRENCY, request.currency.name)
                        .putExtra(EXTRA_AMOUNT, request.amount)
                        .putExtra(EXTRA_NAME, request.senderName)
                        .putExtra(EXTRA_PHONE, request.senderPhone)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                // Android 12+ can refuse a background start (for example if battery is restricted).
                Log.w(TAG, "Foreground service not allowed, speaking without it", e)
                val announcer = fallback ?: Announcer(context).also { fallback = it }
                announcer.announce(request)
            }
        }

        private fun readRequest(intent: Intent): AnnounceRequest? =
            when (intent.getStringExtra(EXTRA_KIND)) {
                "fake" -> AnnounceRequest.Fake
                "unknown" -> AnnounceRequest.Unknown
                "payment" ->
                    AnnounceRequest.Payment(
                        currency = Currency.valueOf(
                            intent.getStringExtra(EXTRA_CURRENCY) ?: return null
                        ),
                        amount = intent.getStringExtra(EXTRA_AMOUNT) ?: return null,
                        senderName = intent.getStringExtra(EXTRA_NAME).orEmpty(),
                        senderPhone = intent.getStringExtra(EXTRA_PHONE).orEmpty()
                    )
                else -> null
            }
    }
}
