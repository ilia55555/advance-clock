# GitHub Releases updates

Public source: https://github.com/ilia55555/advance-clock/releases/latest/download/update.json
No API token, signing key, user identifier, location or application data is uploaded. HTTPS requests reveal the normal network IP to GitHub. No APK is downloaded until the user selects the update menu item.

## Publishing

1. Increase both `versionCode` and four-part `versionName` in `app/build.gradle`.
2. Build a **signed Release APK using the existing production key**. Keep that key private. AAB and Debug builds are not supported by this update channel.
3. Run (Python 3.11+, SDK build-tools on PATH, or provide their full paths):

   ```sh
   python tools/create_update_metadata.py app-release.apk --aapt /path/to/aapt2 --apksigner /path/to/apksigner
   ```

4. Create a public, stable GitHub Release with the printed tag, e.g. `v1.0.0.5`. Upload the signed file under the exact asset name **InfiniTime.apk** and the generated **update.json**. Mark it as the latest release. Do not mark it draft or prerelease. Do not replace APK bytes after generating the JSON.
5. The GitHub repository must stay public for anonymous downloads. No release is published automatically by this implementation. Before the first release the update option stays hidden.

## Behavior

Only the production package checks this channel. Debug keeps its independent package and never offers production APKs. The menu adds a distinctive green final row only for a newer cached release. Android JobScheduler checks approximately every three days with network connectivity; battery restrictions, force stop and offline devices can delay checks. Opening the app also schedules a network-constrained check when due. Failed checks are throttled for a day; stale availability expires after seven days. There is no continuous polling or exact alarm, and no notification.

A download is staged in app-private cache. It is bounded to 150 MiB and ten minutes, with HTTPS-only redirect hosts limited to GitHub and its release CDN. Before export, the app verifies exact size, SHA-256, package name, increasing versionCode, versionName, non-debug/non-test flags and equality of the APK signing certificates with the installed app. Missing certificates or mismatches fail closed. Signing-key rotation is deliberately rejected; migrate the trust policy explicitly if needed.

Android 10+: verified APK is published atomically in `Downloads/InfiniTime`, then the system Downloads/file manager opens. Android 8–9: the system file picker asks where to save the verified APK, then Downloads opens. If an OEM does not handle the Downloads intent, the screen explains the saved location. The user opens the APK and approves installation separately. There is **no REQUEST_INSTALL_PACKAGES permission, installer intent, automatic installation or exported update entry point**.

The signing key remains the security boundary even if release metadata is replaced. Protect it and the GitHub account (2FA/passkeys). Once exported, a public Downloads file is governed by Android/file-manager permissions; the OS independently validates package signatures during installation. No mechanism can promise absolute security.

## Validation limits

Automated tests cover hostile metadata/URLs, version/package/hash/signer constraints and menu availability rules. A real signed upgrade and OEM file-manager handoff must additionally be tested on physical phones; the workspace has no device or production signing key. Background timing is controlled by Android, not guaranteed to the minute.
