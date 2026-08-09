package com.example.screencaster.audio

import android.media.*
import androidx.annotation.RequiresPermission
import android.media.projection.MediaProjection
import android.os.Build
import com.example.screencaster.domain.AudioSettings
import kotlin.math.max

class PcmMixer { fun mix16(a:ShortArray,b:ShortArray,micGain:Float,devGain:Float):ShortArray { val n=max(a.size,b.size); return ShortArray(n){ i -> ((a.getOrElse(i){0}*micGain)+(b.getOrElse(i){0}*devGain)).toInt().coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt()).toShort() } } }

class AudioCaptureFactory {
 @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
 fun microphone(settings: AudioSettings): AudioRecord { val cfg=AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(settings.sampleRate).setChannelMask(if(settings.channels==1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO).build(); val min=AudioRecord.getMinBufferSize(settings.sampleRate,cfg.channelMask,AudioFormat.ENCODING_PCM_16BIT); return AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC).setAudioFormat(cfg).setBufferSizeInBytes(min*2).build() }
 @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
 fun internal(settings: AudioSettings, projection: MediaProjection): AudioRecord? { if(Build.VERSION.SDK_INT<29) return null; val fmt=AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(settings.sampleRate).setChannelMask(if(settings.channels==1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO).build(); val cap=AudioPlaybackCaptureConfiguration.Builder(projection).addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA).addMatchingUsage(android.media.AudioAttributes.USAGE_GAME).build(); val min=AudioRecord.getMinBufferSize(settings.sampleRate,fmt.channelMask,AudioFormat.ENCODING_PCM_16BIT); return AudioRecord.Builder().setAudioFormat(fmt).setAudioPlaybackCaptureConfig(cap).setBufferSizeInBytes(min*2).build() }
}
