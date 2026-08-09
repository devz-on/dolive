package com.example.screencaster.domain

data class StreamPreset(
    val label: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val videoBitrateBps: Int,
    val audioBitrateBps: Int = 128_000
) {
    val summary: String = "${width}x$height • ${fps} FPS • ${BitrateFormatter.mbps(videoBitrateBps)}"

    fun videoSettings() = VideoSettings(width = width, height = height, fps = fps, bitrateBps = videoBitrateBps)
    fun audioSettings(source: AudioSource) = AudioSettings(source = source, bitrateBps = audioBitrateBps)

    companion object {
        val defaults = listOf(
            StreamPreset("Data Saver", 854, 480, 30, 1_500_000, 96_000),
            StreamPreset("Balanced", 1280, 720, 30, 4_000_000),
            StreamPreset("Smooth", 1280, 720, 60, 6_000_000),
            StreamPreset("Full HD", 1920, 1080, 30, 10_000_000),
            StreamPreset("Full HD 60", 1920, 1080, 60, 12_000_000)
        )
    }
}
