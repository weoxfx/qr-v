# SonicPay

A prototype Android app for phone-to-phone, sound-based payment requests —
merchant broadcasts a near-inaudible ultrasonic tone carrying an amount +
payee, customer's phone decodes it and shows a confirm screen before paying.

Built with Kotlin + Jetpack Compose, dark glassmorphic design system.

## What's here right now

- **Home** — pick Merchant or Customer role
- **Merchant screen** — enter amount + VPA, tap to "broadcast" (animated placeholder — real ggwave audio encode comes next)
- **Customer screen** — tap to "listen" (animated placeholder), see an incoming request, confirm before paying

The actual audio encode/decode and BLE proximity discovery aren't wired in
yet — this build proves out the design, navigation, and — most importantly —
the phone-only build pipeline. Real signal processing is the next step.

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

1. Wire in real audio encode/decode (ggwave via Kotlin/JNI, or an equivalent
   pure-Kotlin FSK implementation)
2. Short binary payload (merchant ID + amount) instead of a full URI, to
   minimize transmit time
3. Single-shot broadcast with a manual "resend" button, not looping —
   avoids audio collisions when multiple merchants are nearby
4. BLE proximity handshake as a faster/optional discovery layer alongside
   audio
5. Merchant directory / VPA resolution backend, so only a short token
   travels over the air
