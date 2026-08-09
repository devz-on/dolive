package com.example.screencaster

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.screencaster.domain.AudioSource
import com.example.screencaster.domain.DestinationBuilder
import com.example.screencaster.domain.StreamPreset
import com.example.screencaster.service.CaptureService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }

    @Composable
    fun App() {
        var url by remember { mutableStateOf("rtmps://a.rtmps.youtube.com/live2") }
        var key by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Ready to stream") }
        var mode by remember { mutableStateOf(CaptureService.MODE_STREAM) }
        var preset by remember { mutableStateOf(StreamPreset.defaults[1]) }
        var audioSource by remember { mutableStateOf(AudioSource.MICROPHONE) }

        val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                startService(
                    Intent(this, CaptureService::class.java)
                        .setAction(CaptureService.ACTION_START_CAPTURE)
                        .putExtra(CaptureService.EXTRA_MODE, mode)
                        .putExtra(CaptureService.EXTRA_RESULT_CODE, result.resultCode)
                        .putExtra(CaptureService.EXTRA_DATA, result.data)
                        .putExtra(CaptureService.EXTRA_URL, url)
                        .putExtra(CaptureService.EXTRA_KEY, key.trim())
                        .putExtra(CaptureService.EXTRA_WIDTH, preset.width)
                        .putExtra(CaptureService.EXTRA_HEIGHT, preset.height)
                        .putExtra(CaptureService.EXTRA_FPS, preset.fps)
                        .putExtra(CaptureService.EXTRA_VIDEO_BITRATE, preset.videoBitrateBps)
                        .putExtra(CaptureService.EXTRA_AUDIO_BITRATE, preset.audioBitrateBps)
                        .putExtra(CaptureService.EXTRA_AUDIO_SOURCE, audioSource.name)
                )
                status = "Starting ${if (mode == CaptureService.MODE_RECORD) "recording" else "stream"}…"
            } else {
                status = "Screen capture permission was cancelled."
            }
        }
        val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && audioSource == AudioSource.MICROPHONE) status = "Microphone denied. Choose No audio or grant mic permission."
        }

        fun startCapture(selectedMode: String) {
            mode = selectedMode
            runCatching {
                if (selectedMode != CaptureService.MODE_RECORD) DestinationBuilder.publishUrl(url, key)
                if (audioSource == AudioSource.MICROPHONE || audioSource == AudioSource.INTERNAL_AND_MIC) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                val manager = getSystemService(MediaProjectionManager::class.java)
                projectionLauncher.launch(manager.createScreenCaptureIntent())
                status = "Waiting for Android screen-capture consent…"
            }.onFailure { status = it.message ?: "Invalid stream settings" }
        }

        Scaffold { padding ->
            Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("ScreenCaster", style = MaterialTheme.typography.headlineLarge)
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(status)
                        Text("Profile: ${preset.summary}")
                        Text("Audio: ${audioSource.name.lowercase().replace('_', ' ')}")
                    }
                }
                OutlinedTextField(url, { url = it }, label = { Text("RTMP/RTMPS server URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(key, { key = it }, label = { Text("Stream key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

                Text("Preset")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreamPreset.defaults.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { item ->
                                FilterChip(selected = preset == item, onClick = { preset = item }, label = { Text(item.label) })
                            }
                        }
                    }
                }

                Text("Audio source")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AudioSource.NONE, AudioSource.MICROPHONE, AudioSource.INTERNAL, AudioSource.INTERNAL_AND_MIC).forEach { item ->
                        FilterChip(selected = audioSource == item, onClick = { audioSource = item }, label = { Text(item.name.replace('_', '+')) })
                    }
                }

                Button(onClick = { startCapture(CaptureService.MODE_STREAM) }, modifier = Modifier.fillMaxWidth()) { Text("START STREAM") }
                OutlinedButton(onClick = { startCapture(CaptureService.MODE_RECORD) }, modifier = Modifier.fillMaxWidth()) { Text("START RECORDING") }
                OutlinedButton(onClick = { startCapture(CaptureService.MODE_RECORD_AND_STREAM) }, modifier = Modifier.fillMaxWidth()) { Text("RECORD + STREAM") }
            }
        }
    }
}
