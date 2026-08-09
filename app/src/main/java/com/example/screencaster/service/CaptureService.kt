package com.example.screencaster.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.screencaster.domain.SessionStateMachine
import com.example.screencaster.domain.StreamDestination
import com.example.screencaster.domain.StreamState
import com.example.screencaster.rtmp.RtmpPublisher

class CaptureService : Service() {
    private var wake: PowerManager.WakeLock? = null
    private var publisher: RtmpPublisher? = null
    private var projection: MediaProjection? = null
    private val stateMachine = SessionStateMachine()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        startCaptureForeground("Preparing capture")
        stateMachine.transitionTo(StreamState.PREPARING)
        val url = intent?.getStringExtra(EXTRA_URL).orEmpty()
        val key = intent?.getStringExtra(EXTRA_KEY).orEmpty()
        Thread({ runStart(intent, url, key) }, "capture-service-start").start()
        return START_STICKY
    }

    private fun runStart(intent: Intent?, url: String, key: String) {
        runCatching {
            acquireWakeLock()
            val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
            val data = if (Build.VERSION.SDK_INT >= 33) {
                intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)
            }
            if (code != 0 && data != null) {
                projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data)
            }
            stateMachine.transitionTo(StreamState.CONNECTING)
            publisher = RtmpPublisher(StreamDestination(url, key))
            if (url.isNotBlank() && key.isNotBlank()) publisher?.connect()
            stateMachine.transitionTo(StreamState.STARTING_ENCODER)
            stateMachine.transitionTo(StreamState.LIVE)
            updateNotification("LIVE • RTMP connected")
        }.onFailure { error ->
            stateMachine.transitionTo(StreamState.ERROR)
            updateNotification("Error: ${error.message ?: "capture failed"}")
        }
    }

    private fun acquireWakeLock() {
        if (wake?.isHeld == true) return
        wake = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenCaster:capture")
            .also { it.acquire(6 * 60 * 60 * 1000L) }
    }

    private fun startCaptureForeground(text: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(10, notification(text), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(10, notification(text))
        }
    }

    private fun notification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.presence_video_online)
        .setContentTitle("ScreenCaster")
        .setContentText(text)
        .setOngoing(true)
        .addAction(
            0,
            "STOP",
            PendingIntent.getService(
                this,
                1,
                Intent(this, CaptureService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(10, notification(text))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Active capture", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        stateMachine.transitionTo(StreamState.STOPPING)
        publisher?.close()
        projection?.stop()
        if (wake?.isHeld == true) wake?.release()
        stateMachine.transitionTo(StreamState.STOPPED)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "capture"
        const val ACTION_START_STREAM = "start_stream"
        const val ACTION_STOP = "stop"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        const val EXTRA_URL = "url"
        const val EXTRA_KEY = "key"
    }
}
