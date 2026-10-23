# Release build and code protection

Production version: use the **release** build variant. A debug APK is intentionally
debuggable and must not be distributed as the production app.

Release enables R8 optimization/obfuscation and resource shrinking with
`proguard-android-optimize.txt` plus `app/proguard-rules.pro`. Debugging, native
debugging, shell profiling and test-only installation are disabled. Backup and
cleartext traffic remain disabled. The narrow reflection rules preserve only the
MainActivity members used by MainNoteTabEnhancer. Named sound/prayer resources
are explicitly retained so shrinking does not silently break playback/widgets.

Build and verify from the repository root with JDK 17 and the Android SDK:

```sh
gradle :app:assembleRelease
python tools/verify_release_apk.py
```

The verification reads the compiled APK manifest and the R8 mapping; it checks
the actual output rather than just the Gradle source. CI builds and verifies
Release on both Linux and Windows, and runs the full JVM test suite for Debug
and Release. It also checks packaged manifest components and their constructors,
custom views/constructors in compiled layouts, reflection members, and named
audio/prayer-image resources after shrinking. No production signing key is used by CI.

The local output is **unsigned** at
`app/build/outputs/apk/release/app-release-unsigned.apk`. For distribution, use
Android Studio → Build → Generate Signed App Bundle / APK → release, with the
owner's existing production key (or existing Play App Signing setup). Do not
replace the signing identity for an existing installation. No debug-key fallback
is configured. Keep the keystore, passwords and signing configuration outside Git;
common private-key/configuration filenames are ignored.

Keep `app/build/outputs/mapping/release/mapping.txt` privately for each published
version to retrace crash reports. Build outputs/mappings are ignored by Git and
are not uploaded as public CI artifacts. Verify the signature of the final signed
APK with Android SDK `apksigner verify --verbose` before distributing it. The
verification script can inspect a signed APK via `--apk <path>` as well.

Obfuscation raises the effort needed to understand/decompile the app; it does not
encrypt the program or prevent a device owner from patching and re-signing their
own copy. A patched copy cannot normally update an original installation without
the original signing identity. A rooted/compromised device can bypass additional
checks. Sensitive authorization and private keys must never rely on obfuscation.
No root-blocking or fragile anti-debug timer has been added.

Before shipping, exercise the signed, minified build on actual devices: alarm and
adhan playback, full-screen/lock-screen alerts, notification RemoteViews, all
widgets, and note create/edit/save. JVM tests do not execute the optimized DEX.

## Compatibility verification — 2026-10-24, version 1.0.0.4

- Full Debug JVM suite: 59 suites, 341 tests, no failures/errors/skips.
- Full Release JVM suite: 59 suites, 341 tests, no failures/errors/skips.
- Minified Release build and release-vital lint passed.
- Actual APK/DEX: 325 app classes renamed; all 63 manifest components and their
  public constructors retained; all 35 packaged layouts have available custom
  views and the constructors required for XML inflation.
- Reflected note fields/methods retained with their required names.
- Both named adhan sound resources and all eight named prayer image resources
  present and nonempty after shrinking.
- Both source asset files match the packaged Release bytes exactly.
- Compiled APK security flags verified. The verifier also rejects a debug APK.

No production behavior change was needed during this follow-up check. CI now
runs both complete JVM variants and the expanded packaged-output checks. This
does not substitute for executing the signed, optimized DEX on a physical device;
no emulator or physical Android device was available for this verification.
