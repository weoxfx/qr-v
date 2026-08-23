# SonicPay

A prototype Android app for phone-to-phone, sound-based payment requests —
merchant broadcasts a near-inaudible ultrasonic tone carrying an amount +
payee, customer's phone decodes it and shows a confirm screen before paying.

Built with Kotlin + Jetpack Compose, dark glassmorphic design system.

## What's here right now

- **First launch** — pick Merchant or Customer once; the choice is saved
  and every launch after that opens straight into your role
- **Merchant** — amount field with quick chips (₹10/50/100/200), shop name +
  VPA remembered from Settings, one tap fires a single-shot ultrasonic burst
  (~1.8 s), manual resend = tap again. Warns if media volume is low.
- **Customer** — auto-starts listening the moment the app opens (mic
  permission asked once); decodes the request and pops the confirm card with
  the real VPA + amount; haptic on confirm
- **Settings** — switch role anytime, edit merchant profile, sound self-test
- **History** — last 30 confirmed payments with amounts and timestamps

Design: dark ink glassmorphism — layered translucent panels with hairline
light edges and drifting specular sheens, spring-physics orb and pulse rings,
single mint accent on near-black, tabular-figure typography. No blur-heavy
effects that tank older GPUs — reads as liquid glass on any device.

## The signal chain

Pure Kotlin, no NDK: `sonic/` holds the codec/modem — 8-frequency MFSK in
the 14.2–16.6 kHz band at 44.1 kHz, 40 ms symbols (3 bits each), 4-symbol
sync preamble, CRC16 framing, Goertzel demodulation with preamble-edge fine
sync, timing hypothesis search and duplicate-frame rejection. Covered by JVM
unit tests (`app/src/test/`) including full modulate→demodulate round-trips
under noise and gain changes; CI runs them on every push.

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
