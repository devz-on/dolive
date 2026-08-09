package com.example.screencaster

import android.Manifest
import android.app.Activity
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.screencaster.domain.*
import com.example.screencaster.service.CaptureService

class MainActivity: ComponentActivity(){ override fun onCreate(b:Bundle?){ super.onCreate(b); setContent{ MaterialTheme{ App() } } }
 @Composable fun App(){ var url by remember{ mutableStateOf("rtmps://a.rtmps.youtube.com/live2")}; var key by remember{ mutableStateOf("")}; var status by remember{ mutableStateOf("Ready to stream")}; val projectionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){ r-> if(r.resultCode== Activity.RESULT_OK && r.data!=null){ startService(Intent(this,CaptureService::class.java).setAction(CaptureService.ACTION_START_STREAM).putExtra(CaptureService.EXTRA_RESULT_CODE,r.resultCode).putExtra(CaptureService.EXTRA_DATA,r.data).putExtra(CaptureService.EXTRA_URL,url).putExtra(CaptureService.EXTRA_KEY,key.trim())); status="Preparing foreground stream" } else status="Screen capture permission was cancelled." }; val micLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ }
  Scaffold{ pad-> Column(Modifier.padding(pad).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)){ Text("ScreenCaster", style=MaterialTheme.typography.headlineLarge); ElevatedCard{ Column(Modifier.padding(16.dp)){ Text(status); Text("Profile: 720p • 30 FPS • 4.0 Mbps"); Text("Audio: configurable in service pipeline") } }; OutlinedTextField(url,{url=it}, label={Text("RTMP/RTMPS server URL")}, modifier=Modifier.fillMaxWidth()); OutlinedTextField(key,{key=it}, label={Text("Stream key")}, visualTransformation=PasswordVisualTransformation(), modifier=Modifier.fillMaxWidth()); Button(onClick={ val validation=StreamUrlValidator.validate(url); status=validation.exceptionOrNull()?.message ?: "Waiting for screen capture permission"; if(validation.isSuccess){ micLauncher.launch(Manifest.permission.RECORD_AUDIO); val mgr=getSystemService(MediaProjectionManager::class.java); projectionLauncher.launch(mgr.createScreenCaptureIntent()) } }, modifier=Modifier.fillMaxWidth()){ Text("START STREAM")}; OutlinedButton(onClick={ val mgr=getSystemService(MediaProjectionManager::class.java); projectionLauncher.launch(mgr.createScreenCaptureIntent()) }, modifier=Modifier.fillMaxWidth()){ Text("START RECORDING")}; Text("Permissions & Reliability: screen capture is granted per session; notifications/overlay/battery settings are user controlled."); Text("Diagnostics: AVC capability detection, RTMPS TLS, MediaProjection, AudioPlaybackCapture and network transport are implemented in separate modules.") } }
 }
}
