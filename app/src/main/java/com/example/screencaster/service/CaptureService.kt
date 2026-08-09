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
import com.example.screencaster.domain.AudioSource
import com.example.screencaster.domain.DestinationBuilder
import com.example.screencaster.domain.SessionStateMachine
import com.example.screencaster.domain.StreamState
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.audio.InternalAudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.audio.MixAudioSource
import com.pedro.encoder.input.sources.audio.NoAudioSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.library.base.recording.RecordController
import com.pedro.library.generic.GenericStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureService : Service(), ConnectChecker {
    private var wake: PowerManager.WakeLock? = null
    private var projection: MediaProjection? = null
    private var stream: GenericStream? = null
    private val stateMachine = SessionStateMachine()
    private var currentMode = MODE_STREAM
    private var recordFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startCaptureForeground("Preparing capture")
        stateMachine.transitionTo(StreamState.PREPARING)
        Thread({ runStart(intent) }, "capture-service-start").start()
        return START_STICKY
    }

    private fun runStart(intent: Intent?) {
        runCatching {
            acquireWakeLock()
            val settings = SessionRequest.from(intent)
            currentMode = settings.mode
            val data = extractProjectionData(intent)
            val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
            projection = requireNotNull(getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data)) {
                "Unable to create MediaProjection"
            }
            val endpoint = if (settings.mode != MODE_RECORD) DestinationBuilder.publishUrl(settings.serverUrl, settings.streamKey) else null
            prepareRootEncoder(settings)
            if (settings.mode == MODE_RECORD || settings.mode == MODE_RECORD_AND_STREAM) startRecording()
            if (endpoint != null) {
                stateMachine.transitionTo(StreamState.CONNECTING)
                stream?.startStream(endpoint)
                updateNotification("Connecting to RTMP/RTMPS ingest…")
            } else {
                stateMachine.transitionTo(StreamState.LIVE)
                updateNotification("Recording screen")
            }
        }.onFailure { error ->
            stateMachine.transitionTo(StreamState.ERROR)
            updateNotification("Error: ${error.message ?: "capture failed"}")
            stopSelf()
        }
    }

    private fun prepareRootEncoder(settings: SessionRequest) {
        val projection = requireNotNull(projection)
        val videoSource = ScreenSource(applicationContext, projection)
        val audioSource = when (settings.audioSource) {
            AudioSource.NONE -> NoAudioSource()
            AudioSource.MICROPHONE -> MicrophoneSource()
            AudioSource.INTERNAL -> if (Build.VERSION.SDK_INT >= 29) InternalAudioSource(projection) else NoAudioSource()
            AudioSource.INTERNAL_AND_MIC -> if (Build.VERSION.SDK_INT >= 29) MixAudioSource(projection) else MicrophoneSource()
        }
        stream?.release()
        stream = GenericStream(baseContext, this, NoVideoSource(), audioSource).apply {
            getGlInterface().setForceRender(true, settings.fps)
            changeVideoSource(videoSource)
            val preparedVideo = prepareVideo(
                settings.width,
                settings.height,
                settings.videoBitrate,
                fps = settings.fps,
                iFrameInterval = 2,
                rotation = 0
            )
            val preparedAudio = prepareAudio(
                sampleRate = 44_100,
                isStereo = true,
                bitrate = settings.audioBitrate,
                echoCanceler = settings.audioSource == AudioSource.MICROPHONE || settings.audioSource == AudioSource.INTERNAL_AND_MIC,
                noiseSuppressor = settings.audioSource == AudioSource.MICROPHONE || settings.audioSource == AudioSource.INTERNAL_AND_MIC
            )
            require(preparedVideo) { "Selected video profile is not supported by this device." }
            require(preparedAudio || settings.audioSource == AudioSource.NONE) { "Selected audio profile is not supported by this device." }
        }
    }

    private fun startRecording() {
        val folder = File(getExternalFilesDir(null), "Recordings").apply { mkdirs() }
        val name = "ScreenRecording_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())}.mp4"
        val output = File(folder, name)
        recordFile = output
        stream?.startRecord(output.absolutePath) { status ->
            if (status == RecordController.Status.RECORDING) updateNotification("Recording screen")
        }
    }

    private fun extractProjectionData(intent: Intent?): Intent {
        return if (Build.VERSION.SDK_INT >= 33) {
            requireNotNull(intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)) { "Missing MediaProjection permission data" }
        } else {
            @Suppress("DEPRECATION") requireNotNull(intent?.getParcelableExtra(EXTRA_DATA)) { "Missing MediaProjection permission data" }
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

    override fun onConnectionStarted(url: String) {
        updateNotification("Connecting to streaming server…")
    }

    override fun onConnectionSuccess() {
        stateMachine.transitionTo(StreamState.LIVE)
        updateNotification(if (currentMode == MODE_RECORD_AND_STREAM) "LIVE + recording" else "LIVE • RTMP publishing")
    }

    override fun onConnectionFailed(reason: String) {
        stateMachine.transitionTo(StreamState.ERROR)
        updateNotification("Stream failed: $reason")
        stopSelf()
    }

    override fun onDisconnect() {
        stateMachine.transitionTo(StreamState.STOPPED)
        updateNotification("Stream stopped")
    }

    override fun onAuthError() {
        stateMachine.transitionTo(StreamState.ERROR)
        updateNotification("Streaming server rejected the stream key")
        stopSelf()
    }

    override fun onAuthSuccess() = Unit
    override fun onNewBitrate(bitrate: Long) {
        updateNotification("LIVE • ${(bitrate / 1_000_000.0).formatMbps()} Mbps")
    }

    override fun onDestroy() {
        stateMachine.transitionTo(StreamState.STOPPING)
        runCatching { stream?.stopRecord() }
        runCatching { stream?.stopStream() }
        runCatching { stream?.release() }
        runCatching { projection?.stop() }
        if (wake?.isHeld == true) wake?.release()
        stateMachine.transitionTo(StreamState.STOPPED)
        super.onDestroy()
    }

    private data class SessionRequest(
        val mode: String,
        val serverUrl: String,
        val streamKey: String,
        val width: Int,
        val height: Int,
        val fps: Int,
        val videoBitrate: Int,
        val audioBitrate: Int,
        val audioSource: AudioSource
    ) {
        companion object {
            fun from(intent: Intent?) = SessionRequest(
                mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_STREAM,
                serverUrl = intent?.getStringExtra(EXTRA_URL).orEmpty(),
                streamKey = intent?.getStringExtra(EXTRA_KEY).orEmpty(),
                width = intent?.getIntExtra(EXTRA_WIDTH, 1280) ?: 1280,
                height = intent?.getIntExtra(EXTRA_HEIGHT, 720) ?: 720,
                fps = intent?.getIntExtra(EXTRA_FPS, 30) ?: 30,
                videoBitrate = intent?.getIntExtra(EXTRA_VIDEO_BITRATE, 4_000_000) ?: 4_000_000,
                audioBitrate = intent?.getIntExtra(EXTRA_AUDIO_BITRATE, 128_000) ?: 128_000,
                audioSource = runCatching { AudioSource.valueOf(intent?.getStringExtra(EXTRA_AUDIO_SOURCE).orEmpty()) }.getOrDefault(AudioSource.MICROPHONE)
            )
        }
    }

    private fun Double.formatMbps() = String.format(Locale.US, "%.1f", this)

    companion object {
        private const val CHANNEL_ID = "capture"
        const val ACTION_START_CAPTURE = "start_capture"
        const val ACTION_START_STREAM = ACTION_START_CAPTURE
        const val ACTION_STOP = "stop"
        const val MODE_STREAM = "stream"
        const val MODE_RECORD = "record"
        const val MODE_RECORD_AND_STREAM = "record_and_stream"
        const val EXTRA_MODE = "mode"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        const val EXTRA_URL = "url"
        const val EXTRA_KEY = "key"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_FPS = "fps"
        const val EXTRA_VIDEO_BITRATE = "video_bitrate"
        const val EXTRA_AUDIO_BITRATE = "audio_bitrate"
        const val EXTRA_AUDIO_SOURCE = "audio_source"
    }
}
