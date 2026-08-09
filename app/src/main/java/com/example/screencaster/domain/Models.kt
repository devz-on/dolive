package com.example.screencaster.domain

import java.net.URI
import java.util.Locale

enum class AudioSource { NONE, MICROPHONE, INTERNAL, INTERNAL_AND_MIC }
enum class SessionMode { RECORD, STREAM, RECORD_AND_STREAM }
enum class StreamState { IDLE, PREPARING, WAITING_FOR_PROJECTION_PERMISSION, CONNECTING, STARTING_ENCODER, LIVE, RECONNECTING, STOPPING, STOPPED, ERROR }

data class VideoSettings(val width:Int=1280,val height:Int=720,val fps:Int=30,val bitrateBps:Int=4_000_000,val keyframeSeconds:Int=2,val mime:String="video/avc")
data class AudioSettings(val source:AudioSource=AudioSource.NONE,val sampleRate:Int=44_100,val channels:Int=2,val bitrateBps:Int=128_000,val micGain:Float=1f,val internalGain:Float=1f)
data class StreamDestination(val serverUrl:String="rtmps://a.rtmps.youtube.com/live2", val streamKey:String="")
data class AppSettings(val video:VideoSettings=VideoSettings(), val audio:AudioSettings=AudioSettings(), val destination:StreamDestination=StreamDestination(), val adaptiveBitrate:Boolean=false, val floatingControls:Boolean=false)
data class SessionStats(val durationMs:Long=0,val uploadedBytes:Long=0,val encodedFrames:Long=0,val droppedFrames:Long=0,val reconnects:Int=0,val actualBitrateBps:Long=0,val network:String="Unknown",val rtmpStatus:String="Disconnected")

object BitrateFormatter { fun mbps(bps:Int)=String.format(Locale.US, "%.1f Mbps", bps/1_000_000.0); fun kbps(bps:Int)="${bps/1000} Kbps" }

object StreamUrlValidator {
    fun validate(url:String): Result<URI> = runCatching {
        require(url.isNotBlank()) { "Server URL is required." }
        val uri = URI(url.trim())
        require(uri.scheme == "rtmp" || uri.scheme == "rtmps") { "Only RTMP and RTMPS URLs are supported." }
        require(!uri.host.isNullOrBlank()) { "Server URL must include a host." }
        uri
    }
}

class ReconnectBackoff(private val maxMs:Long=30_000) { fun delayForAttempt(attempt:Int):Long = when(attempt){0,1 -> 500; 2 -> 2_000; 3 -> 5_000; 4 -> 10_000; else -> minOf(maxMs, 10_000L shl (attempt-4).coerceAtMost(4)) } }

object SecretRedactor { private val patterns=listOf(Regex("(?i)(stream[_-]?key=)[^&\\s]+"), Regex("(?i)(key=)[^&\\s]+")); fun redact(input:String)=patterns.fold(input){acc,rx->rx.replace(acc,"$1<redacted>")}.replace(Regex("rtmps?://([^/]+)/([^\\s?]+)")){"${it.value.substringBeforeLast('/')} /<redacted>".replace(" ","")} }
