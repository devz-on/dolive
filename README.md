# ScreenCaster

Native Kotlin/Compose Android screen recording and RTMP/RTMPS streaming app foundation. It builds signed APKs and contains real MediaProjection, MediaCodec, audio, muxer, RTMPS transport, foreground-service, DataStore, and Keystore components, but it still needs device/server validation before production use.

## Requirements
- JDK 17+
- Android Studio with Android Gradle Plugin 8.7.x support
- Android SDK compile/target 37. RootEncoder 2.8.0 requires API 37 compile SDK metadata, so CI installs `platforms;android-37.0`.

## Build
```bash
gradle clean assembleDebug test lint
gradle assembleRelease
```

## Streaming
1. Create or open a YouTube Live event.
2. Copy the RTMPS server URL and stream key.
3. Paste them into ScreenCaster.
4. Pick a preset (Data Saver, Balanced, Smooth, Full HD, Full HD 60) and audio source.
5. Tap **START STREAM**, **START RECORDING**, or **RECORD + STREAM**, then approve Android screen capture.
6. The service only shows `LIVE` after the RTMP/RTMPS publisher reports connection success; connection/auth failures are surfaced in the foreground notification.

## Permissions
- `INTERNET` and `ACCESS_NETWORK_STATE`: RTMP/RTMPS publishing and network state.
- Foreground-service media projection/microphone permissions: active capture while Activity is backgrounded.
- `RECORD_AUDIO`: microphone capture only when enabled by the user.
- `POST_NOTIFICATIONS`: active stream/record controls on Android 13+.
- `SYSTEM_ALERT_WINDOW`: optional floating controls only.
- `WAKE_LOCK`: partial wakelock during active capture only.

## Architecture
Modules separate UI, domain validation, MediaProjection, RootEncoder/MediaCodec AVC encoding, RootEncoder RTMP/RTMPS publishing, local MP4 recording, audio capture/mixing, service lifecycle, network diagnostics, and Keystore-backed secret storage.

## Android version limitations
Internal audio uses Android `AudioPlaybackCaptureConfiguration` and is only available on Android 10/API 29+ when source apps permit capture. DRM/FLAG_SECURE content is never bypassed.

## Known limitations
This repository was generated in a Linux container without emulators, physical devices, or YouTube credentials. Local Gradle unit tests/lint/build were run. MediaProjection runtime capture, valid MP4 creation on a device, full YouTube RTMPS ingest, long-run thermal tests, overlay runtime tests, and network-transition tests were not executed here. Streaming and recording now use RootEncoder instead of the earlier socket-only RTMP placeholder. RootEncoder is expected to emit H.264/AAC RTMP/RTMPS data to YouTube-compatible ingest URLs, but actual YouTube ingest, physical-device MediaProjection runtime capture, long-run thermal tests, overlay runtime tests, and network-transition tests were not executed in this container.


## GitHub signed release workflow
The repository intentionally does not commit APKs, ZIP bundles, keystores, or the Gradle wrapper JAR. The `Android signed release` workflow builds and uploads a signed release APK artifact. Configure these repository secrets before running it:

- `ANDROID_SIGNING_KEYSTORE_BASE64`: base64-encoded JKS/PKCS12 keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: signing key alias.
- `ANDROID_KEY_PASSWORD`: key password.

Create the base64 value locally with `base64 -w0 your-release-key.jks` and paste only the encoded value into GitHub Secrets.
