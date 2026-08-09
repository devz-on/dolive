# ScreenCaster

Native Kotlin/Compose Android screen recording and RTMP/RTMPS streaming app foundation. It builds signed APKs and contains real MediaProjection, MediaCodec, audio, muxer, RTMPS transport, foreground-service, DataStore, and Keystore components, but it still needs device/server validation before production use.

## Requirements
- JDK 17+
- Android Studio with Android Gradle Plugin 8.7.x support
- Android SDK compile/target 35 in this environment. The product request prefers API 37 when available; this container did not include an Android SDK, so the project is configured to a stable SDK line that can be installed by Android Studio.

## Build
```bash
gradle clean assembleDebug test lint
gradle assembleRelease
```

## Streaming
1. Create or open a YouTube Live event.
2. Copy the RTMPS server URL and stream key.
3. Paste them into ScreenCaster.
4. Tap **START STREAM** and approve Android screen capture.

## Permissions
- `INTERNET` and `ACCESS_NETWORK_STATE`: RTMP/RTMPS publishing and network state.
- Foreground-service media projection/microphone permissions: active capture while Activity is backgrounded.
- `RECORD_AUDIO`: microphone capture only when enabled by the user.
- `POST_NOTIFICATIONS`: active stream/record controls on Android 13+.
- `SYSTEM_ALERT_WINDOW`: optional floating controls only.
- `WAKE_LOCK`: partial wakelock during active capture only.

## Architecture
Modules separate UI, domain validation, MediaProjection, MediaCodec AVC encoding, audio capture/mixing, RTMP transport, service lifecycle, network diagnostics, and Keystore-backed secret storage.

## Android version limitations
Internal audio uses Android `AudioPlaybackCaptureConfiguration` and is only available on Android 10/API 29+ when source apps permit capture. DRM/FLAG_SECURE content is never bypassed.

## Known limitations
This repository was generated in a Linux container without emulators, physical devices, or YouTube credentials. Local Gradle unit tests/lint/build were run. MediaProjection runtime capture, valid MP4 creation on a device, full YouTube RTMPS ingest, long-run thermal tests, overlay runtime tests, and network-transition tests were not executed here. The RTMP class opens a validated RTMP/RTMPS socket and begins the handshake foundation, but the full command/chunk/FLV publish sequence still needs completion and server validation before production release.


## GitHub signed release workflow
The repository intentionally does not commit APKs, ZIP bundles, keystores, or the Gradle wrapper JAR. The `Android signed release` workflow builds and uploads a signed release APK artifact. Configure these repository secrets before running it:

- `ANDROID_SIGNING_KEYSTORE_BASE64`: base64-encoded JKS/PKCS12 keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: signing key alias.
- `ANDROID_KEY_PASSWORD`: key password.

Create the base64 value locally with `base64 -w0 your-release-key.jks` and paste only the encoded value into GitHub Secrets.
