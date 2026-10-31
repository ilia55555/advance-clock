# Foreground-only GitHub Releases updates

The dedicated public release repository is **https://github.com/infinitimeilia/infinitime**, configured in `update_repository` in `app/src/main/res/values/strings.xml`. Version `1.0.0.5` (code 5) is the first build connected to this repository. For an end-to-end upgrade test, install that signed build, then publish `1.0.0.6` (code 6) signed with the same key. Earlier builds with an empty repository cannot discover this channel and need a manual initial upgrade.

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

Only the production package checks this channel. When the main app screen resumes with an active network advertising Internet access, it checks the small JSON. Rapid resumes are throttled to five minutes. There is **no periodic job, alarm, connectivity listener, retry timer, update notification or background checking**. Old JobScheduler IDs 8204/8205 are cancelled on migration/app startup. Leaving the main screen interrupts the check, disconnects it and prevents callbacks from opening a sheet later. An already visible sheet stays available on return; active downloads are cancelled on backgrounding and can be restarted. Automatic offline/error cases are silent. Android network validation is not required: VPNs and some networks can reach GitHub without receiving the Android VALIDATED capability. HTTPS certificate and release validation remain mandatory.

A new version opens an optional bottom sheet at most once per version/source. Only Close and Later dismiss it; Back and outside taps do not. The distinct final menu option remains available for reopening the sheet manually. The APK is downloaded only after tapping **Download update**, inside the same sheet with byte counts, percentage, a rounded progress bar and a verification stage. Download is physically right and Later left in every language. No separate download activity is launched. No forced update and no automatic APK download. Cached offers expire after seven days and cannot cross repository changes.

No API token, signing key, user identifier, location or application data is uploaded. HTTPS requests reveal the normal network IP to GitHub.

## Manual check and diagnostics

The main About screen offers **Check for updates** and a link to the public GitHub releases page. A user-requested check bypasses the five-minute throttle and can reopen the new-version sheet even if it was dismissed before. It reports current version, offline/network/secure-connection errors, missing metadata, invalid metadata, unavailable server or unsupported Debug package. It never automatically downloads an APK. Leaving About cancels the check.

Download failures distinguish missing release APK (check the `vN.N.N.N` release tag), hash mismatch, incompatible signer/package, connection issues and other download/storage failures. The Latest release must actually contain both JSON and APK, and the version-specific APK URL derived from metadata must resolve. Merely uploading JSON tagged `v1.0.0.6` under a release tagged `main1` does not create the expected APK path.

## Download security and user-confirmed installation

A user-requested APK download is staged in app-private cache. It is bounded to 150 MiB and ten minutes, with HTTPS-only redirect hosts limited to GitHub and its release CDN. Before export, the app verifies exact size, SHA-256, package name, increasing versionCode, versionName, non-debug/non-test flags and equality of the APK signing certificates with the installed app. Missing certificates or mismatches fail closed. Signing-key rotation is deliberately rejected; migrate the trust policy explicitly if needed.

Version `1.0.0.7` (code 7) adds user-confirmed installation. Only an APK that passed verification is copied into app-private staging and exposed through a non-exported read-only provider (`${applicationId}.updates`). The installer gets only a temporary read grant; path traversal, external writes and arbitrary external download URLs are rejected. Old staged files are cleaned after seven days.

The app declares `REQUEST_INSTALL_PACKAGES`. If Android has not authorized installation from this app, it opens that app-specific settings page and rechecks permission on return. Denial produces an explicit message and a file-manager button. Approval opens the system APK installer, which still requires the user to confirm installation; there is no silent installation or permission bypass.

On Android 10+, a verified copy is also published atomically in `Download/InfiniTime`. File-manager navigation tries a standard folder intent, the system Files category and launchable Samsung, Xiaomi, Huawei, Honor or Google Files apps. DownloadManager is not used because its list need not include MediaStore exports. No OEM-independent API guarantees a specific file-manager UI; if none resolves, a manual location message is shown. On older Android or if public export fails, the manual fallback first asks the user where to save the verified APK using the system document picker. No broad storage or QUERY_ALL_PACKAGES permission is requested.


The signing key remains the security boundary even if release metadata is replaced. Protect it and the GitHub account (2FA/passkeys). Once exported, a public Downloads file is governed by Android/file-manager permissions; the OS independently validates package signatures during installation.

Automated checks cover metadata, destination restrictions, hash/package/version/signers, removal of background scheduling and optional sheet actions. A real signed upgrade and OEM file-manager handoff still require physical-phone testing; the workspace has no device or production signing key.

If a published APK is replaced (including rebuilding the same version), regenerate `update.json`: its size and SHA-256 must match the exact new bytes. A mismatch is rejected and the staged file is deleted. The modal reads the localized `releaseNotes` field embedded in `update.json`; changing only a separate notes asset requires regenerating that metadata too.
