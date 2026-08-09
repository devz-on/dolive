package com.example.screencaster.recording

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/** Writes encoded AVC/AAC samples to a standards-compliant MP4 with monotonic PTS checks. */
class Mp4MuxerSink(private val outputFile: File) {
    private var muxer: MediaMuxer? = null
    private var videoTrack = -1
    private var audioTrack = -1
    private var started = false
    private var lastVideoPtsUs = Long.MIN_VALUE
    private var lastAudioPtsUs = Long.MIN_VALUE

    fun open() {
        require(!started) { "Muxer is already started" }
        outputFile.parentFile?.mkdirs()
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun setVideoFormat(format: MediaFormat) {
        check(videoTrack == -1) { "Video track already added" }
        videoTrack = requireMuxer().addTrack(format)
        startIfReady()
    }

    fun setAudioFormat(format: MediaFormat) {
        check(audioTrack == -1) { "Audio track already added" }
        audioTrack = requireMuxer().addTrack(format)
        startIfReady()
    }

    fun writeVideoSample(data: ByteArray, info: MediaCodec.BufferInfo) {
        if (!started || info.size <= 0) return
        if (info.presentationTimeUs < lastVideoPtsUs) return
        lastVideoPtsUs = info.presentationTimeUs
        writeSample(videoTrack, data, info)
    }

    fun writeAudioSample(data: ByteArray, info: MediaCodec.BufferInfo) {
        if (!started || info.size <= 0 || audioTrack == -1) return
        if (info.presentationTimeUs < lastAudioPtsUs) return
        lastAudioPtsUs = info.presentationTimeUs
        writeSample(audioTrack, data, info)
    }

    private fun writeSample(track: Int, data: ByteArray, info: MediaCodec.BufferInfo) {
        require(track >= 0) { "Track not configured" }
        val buffer = java.nio.ByteBuffer.wrap(data)
        val copy = MediaCodec.BufferInfo().also { it.set(0, data.size, info.presentationTimeUs, info.flags) }
        requireMuxer().writeSampleData(track, buffer, copy)
    }

    private fun startIfReady() {
        if (!started && videoTrack >= 0) {
            requireMuxer().start()
            started = true
        }
    }

    fun close() {
        val m = muxer ?: return
        runCatching { if (started) m.stop() }
        runCatching { m.release() }
        muxer = null
        started = false
    }

    private fun requireMuxer() = muxer ?: error("Muxer is not open")
}
