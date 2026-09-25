# Unique Rural Fund — Android App (Source Backup)

This repo holds the **buildable source** of the `com.uniqueruralfund.app` Android app — a Capacitor wrapper that loads the live site at https://uniqueruralfund.github.io in a WebView, with one piece of native code added on top (see below). It does **not** hold `node_modules/`, Gradle build caches, or any signing key — those are excluded on purpose (see `android/.gitignore` and `.gitignore` conventions) and must be restored/regenerated before a rebuild.

## What's here
- `capacitor.config.json`, `package.json`, `package-lock.json` — Capacitor project config and exact dependency versions.
- `android/` — the native Android project (manifest, Gradle files, resources, and the one custom Java file).
- `www/index.html` — a placeholder web asset (the app actually loads the **remote** site via `capacitor.config.json`'s `server.url`, not this file — Capacitor still requires *some* `webDir` to exist).

## The one piece of native code
`android/app/src/main/java/com/uniqueruralfund/app/MainActivity.java` overrides Capacitor's `BridgeWebViewClient` so that any URL with a **non-http(s) scheme** (`upi://`, `tel:`, `mailto:`, `intent:`, …) is launched via `Intent.ACTION_VIEW` instead of the WebView silently failing to navigate to it. Falls back to a Toast ("কোনো UPI অ্যাপ পাওয়া যায়নি") if `ActivityNotFoundException` is thrown. Normal `http(s)` navigation passes through to `super`, so Capacitor's own bridge/asset-loading logic is untouched.

This exists because a "📲 UPI QR কোড দেখান" style feature was tried on the website — a plain `window.location.href = 'upi://...'` works in a normal mobile browser (Chrome/Safari hand it off to the OS) but does **nothing** inside a bare Capacitor WebView without this override.

## Signing — the part that needs care
The shipped, currently-installed app is signed with the **plain default Android debug keystore**, not a dedicated release key:
- Keystore file: `C:\Users\PRASUN\.android\debug.keystore` (on the machine this was built on)
- Alias: `androiddebugkey`
- Password (both keystore and key): `android` (the universal Android SDK default)
- Signer cert SHA-256: `31:E9:84:12:9F:D1:D0:E3:96:0C:CE:A9:E3:89:AD:D4:CC:F2:68:FC:71:01:74:DD:B2:9A:83:83:8D:03:44:66`

**Important:** this is the *specific file* at that path, not "any debug keystore" — a freshly auto-generated `debug.keystore` on a different machine has a different random key and will NOT produce a matching signature. To ship an update that Android will install over the currently-installed app (rather than as a conflicting app), you need this *exact* keystore file.

This keystore is currently **only on the original build machine** — it was never backed up elsewhere (unlike the Alcove SOP project's dedicated release key, which lives in a private Drive folder). **Recommended next step: back this file up somewhere durable** (e.g. a private Drive folder, following the same pattern used for the Alcove SOP keystore) before it's at risk of being lost. Never commit it to this (public) repo.

If the keystore is ever genuinely lost, the practical fallback is: generate a new keystore, rebuild, and have every user uninstall the old app and install the new one fresh (loses no data, since the app itself holds no local state — everything lives on the server).

## Rebuild steps
1. Ensure Node.js and the Android SDK (build-tools + a platform, e.g. android-34/35/36) are available. This project was originally built with Android SDK components at `C:\Android\Sdk`.
2. `npm install` (restores `node_modules` from `package-lock.json`).
3. Make your code change (most likely in `MainActivity.java`, or a version bump in `android/app/build.gradle`).
4. **Bump `versionCode` in `android/app/build.gradle`** — must be strictly greater than whatever is currently installed on users' phones, or Android will refuse to install it as an update.
5. Build: `cd android && .\gradlew.bat assembleDebug` (Gradle wrapper handles its own Gradle version — no separate Gradle install needed).
6. Sign the output (`android/app/build/outputs/apk/debug/app-debug.apk`) with the debug keystore above, if `assembleDebug` didn't already auto-sign it with the machine's default debug keystore (it does by default when that keystore already exists at the standard path).
7. Verify before shipping: `apksigner verify -v --print-certs <apk>` (expect the SHA-256 above) and `aapt2 dump badging <apk>` (confirm `applicationId: 'com.uniqueruralfund.app'` and the new `versionCode`). Both tools live under `<Android SDK>\build-tools\<version>\`.
8. Distribute the new APK the same way as before — e.g. re-upload it to `uniqueruralfund.github.io/UniqueRuralFund.apk` via the GitHub Contents API so the "📱 অ্যাপ ডাউনলোড করুন" link on the Welcome screen serves the new build.

## What does NOT need a rebuild
Any change to the actual website content/behavior — `Dashboard.html`, the Apps Script backend, styling, new features — needs **zero** app rebuild, since the app just loads that live site remotely. Only *native* changes (permissions, the WebViewClient logic, app icon, manifest, Capacitor plugin additions) require going through the steps above.
