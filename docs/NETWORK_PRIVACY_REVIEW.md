# Network and privacy review — 2026-10-31

## Scope and changes

The About update link now opens exactly `https://github.com/infinitimeilia/infinitime`. Update checks and downloads pin the same repository in code and reject configuration, metadata-supplied URL or redirect substitutions outside the approved release paths/CDN. A private preference or JSON field cannot select a new repository. The APK still has to match size, SHA-256, package, increasing version and the installed signing certificate before installation is offered.

All app-managed HTTP creation is centralized in `ReadOnlyNetwork`. It permits HTTPS GET/HEAD only, rejects request bodies and output streams, restricts headers to fixed public values, and prevents automatic redirects. Update redirects are individually validated. Location endpoints permit only known city-search/time-zone paths and necessary query fields, with coordinate validation, duplicate-key rejection and response size limits. System CA trust is explicit; plaintext and user-added CA trust are not enabled. Native audio/video readers reject HTTP/RTSP and other remote schemes so they cannot bypass this HTTP boundary.

No application file-upload API, analytics SDK or direct POST body upload was found in the original source. However, direct HTTP creation and native remote media were less constrained, and speech used an engine that could process audio online. Those paths are now restricted as described here.

## Explicit online voice exception

The owner explicitly requested Google's own online voice window. The app launches `ACTION_RECOGNIZE_SPEECH` pinned to `com.google.android.googlequicksearchbox` only after the user presses the speech button. Google records/processes voice using its own app permissions and may send it online under Google's policy. Only language/model options are passed; existing typed text, notes, files and attachments are not included. The caller app no longer declares RECORD_AUDIO or captures microphone buffers itself. No fallback to an arbitrary recognition service is used if Google is absent. Recognition languages are queried only from the Google package.

This is an intentional exception to the otherwise download-only app HTTP policy. It is not accurate to claim that Google's voice recognition never uploads audio.

## Data that may leave the device

- Official update requests: fixed release paths plus ordinary IP, TLS and HTTP connection metadata. No account, contacts, notes, recordings, attachment content or installed-app inventory is included.
- Location exception: the city search text or latitude/longitude needed to determine location/time zone. Existing offline city data is used where available. Android Geocoder/location services may perform the lookup in their own processes; app-level permission gates still apply. Internet location lookups necessarily disclose their query or coordinates to that provider.
- Explicit Google voice: Google may receive/process the recording. The app receives recognized text.
- A browser or external file viewer selected by the user is a separate app. It may have its own network behavior. The app cannot impose a device-wide firewall on other apps, keyboards, cloud providers or a compromised OS.

Manual APK export requests local-only storage and rejects cloud document-provider destinations before writing file bytes. App-private media/crop/cache writes and the app-created Downloads export are local filesystem/provider operations, not network upload bodies. Buttons previously called “upload” in sound selection import a user-selected local audio file; they do not upload it to a server.

## Security boundary and validation

Android private storage and non-exported providers protect mutable app data from ordinary third-party apps. A modified/re-signed APK cannot update the genuine app with a different key. Keep the production key and GitHub account secure: a compromised publisher can alter public descriptions or cause denial of service, and a compromised signing key or rooted device is outside these protections. No absolute “unhackable” claim is made.

Automated tests cover hostile download links, cross-repository redirects, forbidden methods/bodies/headers, extra/invalid location fields, cloud export rejection, native remote-media rejection, Google UI package pinning, no private-text extras, microphone-permission removal and unchanged APK verification. A source-level regression guard rejects new direct HTTP/socket calls outside the central boundary or unguarded native media streams. Existing Debug/Release tests and release verification are run as part of this change. No physical-phone traffic capture or independent penetration test was performed in this workspace.
