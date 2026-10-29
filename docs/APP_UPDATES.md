# Foreground-only GitHub Releases updates

The owner will provide a **new public release repository**. Until then `update_repository` in `app/src/main/res/values/strings.xml` is intentionally empty: no update HTTP request, old-repository fallback or update offer is made. After receiving the actual link, set it to the exact canonical `https://github.com/OWNER/REPOSITORY` (no trailing slash). It is not user-editable and accepts only a GitHub repository path.

## Publishing

1. Increase both `versionCode` and four-part `versionName` in `app/build.gradle`.
2. Build a **signed Release APK using the existing production key**. Keep that key private. AAB and Debug builds are not supported by this update channel.
3. Create an optional `release-notes.json`, for example:

   ```json
   {
     "title": {
       "fa": "نسخهٔ تازهٔ InfiniTime آماده است",
       "en": "A new InfiniTime version is ready",
       "ar": "يتوفر إصدار جديد من InfiniTime"
     },
     "releaseNotes": {
       "fa": "بهبود تقویم\nرفع مشکلات اعلان",
       "en": "Calendar improvements\nNotification fixes",
       "ar": "تحسين التقويم\nإصلاح الإشعارات"
     }
   }
   ```

4. Run (Python 3.11+, SDK build-tools on PATH, or provide their full paths):

   ```sh
   python tools/create_update_metadata.py app-release.apk --notes release-notes.json --aapt /path/to/aapt2 --apksigner /path/to/apksigner
   ```

5. Create a public stable GitHub Release in the dedicated repository with the printed tag, e.g. `v1.0.0.5`. Upload the signed file under the exact asset name **InfiniTime.apk** and generated **update.json**. Mark it as the latest stable release. Do not replace APK bytes after generating the JSON. No release is published automatically.

The app reads `REPOSITORY/releases/latest/download/update.json`. JSON contains `versionCode`, `versionName`, `tag`, `size`, `sha256`, and optionally `title`/`releaseNotes`. These text fields accept a plain string or language-to-string object, with English fallback. They are displayed as plain text, never HTML, scripts or links. Metadata is capped at 16 KiB. The menu always has the app-translated “Update available” label; the bottom sheet shows the custom title and new features.

## Foreground behavior and battery

Only the production package checks this channel. When the main app screen resumes with a validated Internet connection, it checks the small JSON. Rapid resumes are throttled to five minutes. There is **no periodic job, alarm, connectivity listener, retry timer, update notification or background checking**. Old JobScheduler IDs 8204/8205 are cancelled on migration/app startup. Leaving the main screen interrupts the request, disconnects it and prevents callbacks from opening a sheet later. Offline and error cases are silent.

A new version opens an optional bottom sheet at most once per version/source. Back, outside tap, close and Later all dismiss it. The distinct final menu option remains available for reopening the sheet manually. The APK is downloaded only after tapping **Download update**. No forced update and no automatic APK download. Cached offers expire after seven days and cannot cross repository changes.

No API token, signing key, user identifier, location or application data is uploaded. HTTPS requests reveal the normal network IP to GitHub.

## Download security and manual installation

A user-requested APK download is staged in app-private cache. It is bounded to 150 MiB and ten minutes, with HTTPS-only redirect hosts limited to GitHub and its release CDN. Before export, the app verifies exact size, SHA-256, package name, increasing versionCode, versionName, non-debug/non-test flags and equality of the APK signing certificates with the installed app. Missing certificates or mismatches fail closed. Signing-key rotation is deliberately rejected; migrate the trust policy explicitly if needed.

Android 10+: verified APK is published atomically in `Downloads/InfiniTime`, then the system Downloads/file manager opens. Android 8–9: the system file picker asks where to save the verified APK, then Downloads opens. If an OEM does not handle the Downloads intent, the screen explains the saved location. The user opens the APK and approves installation separately. There is **no REQUEST_INSTALL_PACKAGES permission, installer intent, automatic installation or exported update entry point**.

The signing key remains the security boundary even if release metadata is replaced. Protect it and the GitHub account (2FA/passkeys). Once exported, a public Downloads file is governed by Android/file-manager permissions; the OS independently validates package signatures during installation.

Automated checks cover metadata, destination restrictions, hash/package/version/signers, removal of background scheduling and optional sheet actions. A real signed upgrade and OEM file-manager handoff still require physical-phone testing; the workspace has no device or production signing key.
