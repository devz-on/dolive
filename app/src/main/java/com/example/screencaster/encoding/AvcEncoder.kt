package com.example.screencaster.encoding

import android.media.*
import android.os.Bundle
import android.view.Surface
import com.example.screencaster.domain.VideoSettings

class AvcEncoder(private val settings: VideoSettings) {
    private var codec: MediaCodec? = null
    var inputSurface: Surface? = null; private set
    fun start(onFormat:(MediaFormat)->Unit, onSample:(ByteArray,MediaCodec.BufferInfo)->Unit) {
        val format = MediaFormat.createVideoFormat(settings.mime, settings.width, settings.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, settings.bitrateBps); setInteger(MediaFormat.KEY_FRAME_RATE, settings.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, settings.keyframeSeconds)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
        }
        codec = MediaCodec.createEncoderByType(settings.mime).apply { configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); inputSurface=createInputSurface(); start() }
        Thread({ drain(onFormat,onSample) }, "avc-drain").start()
    }
    fun requestBitrate(bps:Int){ codec?.setParameters(Bundle().apply{ putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE,bps) }) }
    private fun drain(onFormat:(MediaFormat)->Unit, onSample:(ByteArray,MediaCodec.BufferInfo)->Unit){ val c=codec ?: return; val info=MediaCodec.BufferInfo(); var running=true; while(running){ val idx=c.dequeueOutputBuffer(info,10_000); when { idx==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> onFormat(c.outputFormat); idx>=0 -> { c.getOutputBuffer(idx)?.let{ buf -> if(info.size>0){ val data=ByteArray(info.size); buf.position(info.offset); buf.limit(info.offset+info.size); buf.get(data); onSample(data, MediaCodec.BufferInfo().also{ it.set(0,data.size,info.presentationTimeUs,info.flags) }) } }; running = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0; c.releaseOutputBuffer(idx,false) } } } }
    fun stop(){ runCatching{ codec?.signalEndOfInputStream() }; runCatching{ inputSurface?.release() }; runCatching{ codec?.stop() }; runCatching{ codec?.release() }; codec=null }
}
