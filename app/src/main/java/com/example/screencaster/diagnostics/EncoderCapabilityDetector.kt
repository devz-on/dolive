package com.example.screencaster.diagnostics

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

data class EncoderCapability(
    val name: String,
    val mime: String,
    val hardwareAccelerated: Boolean,
    val maxWidth: Int,
    val maxHeight: Int,
    val supportsCbr: Boolean,
    val supportedFps: List<Int>
)

class EncoderCapabilityDetector {
    fun avcEncoders(): List<EncoderCapability> {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        return codecs.filter { it.isEncoder && it.supportedTypes.any { type -> type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) } }
            .mapNotNull { codec -> runCatching { toCapability(codec) }.getOrNull() }
    }

    fun supports(width: Int, height: Int, fps: Int): Boolean = avcEncoders().any { capability ->
        width <= capability.maxWidth && height <= capability.maxHeight && fps in capability.supportedFps
    }

    private fun toCapability(codec: MediaCodecInfo): EncoderCapability {
        val caps = codec.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val video = requireNotNull(caps.videoCapabilities) { "Missing AVC video capabilities for ${codec.name}" }
        val encoder = requireNotNull(caps.encoderCapabilities) { "Missing AVC encoder capabilities for ${codec.name}" }
        val fpsCandidates = listOf(15, 24, 25, 30, 48, 60)
        return EncoderCapability(
            name = codec.name,
            mime = MediaFormat.MIMETYPE_VIDEO_AVC,
            hardwareAccelerated = if (Build.VERSION.SDK_INT >= 29) codec.isHardwareAccelerated else !codec.name.startsWith("OMX.google"),
            maxWidth = video.supportedWidths.upper,
            maxHeight = video.supportedHeights.upper,
            supportsCbr = encoder.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR),
            supportedFps = fpsCandidates.filter { video.areSizeAndRateSupported(1280, 720, it.toDouble()) || it <= 30 }
        )
    }
}
