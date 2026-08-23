# SonicPay

A prototype Android app for phone-to-phone, sound-based payment requests —
merchant broadcasts a near-inaudible ultrasonic tone carrying an amount +
payee, customer's phone decodes it and shows a confirm screen before paying.

Built with Kotlin + Jetpack Compose, dark glassmorphic design system.

## What's here right now

- **Home** — pick Merchant or Customer role
- **Merchant screen** — enter amount + VPA, tap to broadcast: the request is
  FSK-modulated into a near-inaudible ultrasonic tone (16.4–18.8 kHz band,
  44.1 kHz, pure-Kotlin DSP — no NDK) and played through the speaker
- **Customer screen** — requests mic permission, captures audio with
  `AudioRecord`, decodes the tone with a Goertzel-based demodulator
  (preamble sync + CRC16 validation), and shows the confirm card with the
  real decoded VPA + amount

Broadcast is single-shot with manual resend; duplicate frames within ~2.5s
are ignored on the receiving side.

The codec/modem logic (`app/src/main/java/com/sonicpay/app/sonic/`) is pure
Kotlin and covered by JVM unit tests (`./gradlew :app:testDebugUnitTest`),
including full modulate→demodulate round-trips under noise and gain changes.
BLE proximity discovery and a VPA resolution backend aren't wired in yet.

## Building the APK — entirely from your phone

You don't need Android Studio. GitHub's servers do the compiling for you.

1. **Create a GitHub repo** (from the GitHub app or mobile browser):
   - New repository → name it `sonicpay` → keep it empty (no README/gitignore)
2. **Upload these files** into that repo. Easiest way on mobile:
   - Open the repo → "Add file" → "Upload files"
   - Upload the whole folder structure (GitHub's mobile web uploader accepts
     multiple files/folders when you pick "choose files" from a file manager
     app that shows the extracted project folder)
   - Alternative if drag-upload is clunky on mobile: install the **Working
     Copy** (iOS) or **GitJournal / MGit** (Android) app, clone your empty
     repo, copy this project folder into it, then commit + push from the app
3. **Check the build:** go to the **Actions** tab in your repo — a workflow
   called "Build APK" should already be running (it triggers automatically
   on push).
4. **Download the APK:** once the workflow finishes (green check, a couple
   of minutes), open that run → scroll to **Artifacts** → download
   `SonicPay-debug-apk` → unzip it (most phone file managers can unzip) →
   you'll have `app-debug.apk`.
5. **Install it:** tap the APK file → Android will ask to allow installs
   from this source once → Install.

## Next steps (in order)

1. BLE proximity handshake as a faster/optional discovery layer alongside
   audio
2. Merchant directory / VPA resolution backend, so only a short token
   travels over the air
3. Field-tune the modem: adaptive symbol duration, error correction
   (e.g. Reed-Solomon), and speaker/mic profile tweaks for longer range
