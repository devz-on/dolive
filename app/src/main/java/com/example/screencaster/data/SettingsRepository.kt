package com.example.screencaster.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.screencaster.domain.AppSettings
import com.example.screencaster.domain.AudioSettings
import com.example.screencaster.domain.VideoSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val width = intPreferencesKey("video_width")
        val height = intPreferencesKey("video_height")
        val fps = intPreferencesKey("video_fps")
        val videoBitrate = intPreferencesKey("video_bitrate")
        val audioBitrate = intPreferencesKey("audio_bitrate")
        val adaptive = booleanPreferencesKey("adaptive_bitrate")
        val floating = booleanPreferencesKey("floating_controls")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            video = VideoSettings(
                width = prefs[Keys.width] ?: 1280,
                height = prefs[Keys.height] ?: 720,
                fps = prefs[Keys.fps] ?: 30,
                bitrateBps = prefs[Keys.videoBitrate] ?: 4_000_000
            ),
            audio = AudioSettings(bitrateBps = prefs[Keys.audioBitrate] ?: 128_000),
            adaptiveBitrate = prefs[Keys.adaptive] ?: false,
            floatingControls = prefs[Keys.floating] ?: false
        )
    }

    suspend fun saveVideo(video: VideoSettings) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.width] = video.width
            prefs[Keys.height] = video.height
            prefs[Keys.fps] = video.fps
            prefs[Keys.videoBitrate] = video.bitrateBps
        }
    }
}
