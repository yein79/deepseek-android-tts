# DeepSeek Android TTS prototype

Experimental Android system TTS engine scaffold intended to call a compatible ds-tts HTTP service and return WAV audio to Android apps.

## Important status

**This is not yet a working DeepSeek voice bridge, and it is not verified to work with Kyobo Library.**

The upstream `liceses/dsh-ds-tts` project uses a logged-in browser to place text into a DeepSeek assistant message and then requests official audio. Its documented browser automation targets Windows. An Android-compatible service/bridge is still required.

Even if this app builds and is selected as Android's default TTS engine, Kyobo Library may continue using its bundled DioTTS engine. That requires separate device testing.

Upstream reference: https://github.com/liceses/dsh-ds-tts
Android TTS service API: https://developer.android.com/reference/android/speech/tts/TextToSpeechService

## Build

GitHub Actions is configured to build a debug APK on pushes. If the workflow succeeds, download the APK from the workflow's Artifacts section. A successful build proves compilation only, not end-to-end DeepSeek audio.

## Safety

Only configure a service URL you trust. Do not expose a local TTS bridge to the public internet. Never commit DeepSeek cookies, tokens, or session data.
