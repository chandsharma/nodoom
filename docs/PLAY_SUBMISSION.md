# Google Play submission record

This file keeps the Play Console answers aligned with the shipped binary. It is not a substitute for reading the current policy text shown in Play Console.

## Artifact

| Field | Value |
|---|---|
| Application ID | `com.chandsharma.nodoom` |
| Version | `1.0.0` (`versionCode 7`) |
| Minimum Android | Android 14 / API 34 |
| Target Android | Android 16 / API 36 |
| Signed AAB | `dist/1.0.0/NoDoom-1.0.0.aab` (local/Release asset; not committed) |
| AAB SHA-256 | `9ee2eadace5784c24525ea7286bcf1eb0365e6dff3fca9731f1ce5e5a938fd18` |
| Upload certificate SHA-256 | `5822e80c3db9b629f0cd4e8a35fd274da98892a54ee60d5d0cb8fd653f1db28e` |
| Category | Productivity |
| Ads | No |
| App account | None |
| App access | All functionality is available without credentials |
| Privacy policy | <https://chandsharma.github.io/nodoom/privacy/> |

## Data safety

- Data collected: **No**.
- Data shared: **No**.
- Data is encrypted in transit: **Not applicable; the app has no Internet permission and transmits no data**.
- Account creation/deletion: **Not applicable; the app has no account system**.
- Independent security review: do not claim one unless a qualifying review is completed.

Locally persisted settings are documented in [`PRIVACY.md`](PRIVACY.md). They remain on-device and are excluded from Android backup/device transfer.

## Accessibility declaration

- The app uses `AccessibilityService` for a non-disability core feature; `isAccessibilityTool` remains `false`.
- Purpose: identify a full-screen Reel viewer in the official Instagram app and intercept the vertical gesture that would advance to another Reel.
- Foreground package changes are used to immediately clear the gesture layer outside Instagram. Other apps' window content is not traversed.
- Android System UI is recognised so the notification shade and Quick Settings receive their own gestures.
- The app does not use Accessibility to change settings, bypass controls, perform social actions, prevent disabling or prevent uninstalling.
- All processing happens on-device; Accessibility screen content is not stored or shared.
- A prominent in-app disclosure appears immediately before the affirmative **I agree · continue** action that opens Android Accessibility settings.
- The store listing includes the same disclosure in plain language.

Play Console may request a demonstration video showing the disclosure, permission flow and one-Reel behavior. Record it from the final signed build with a non-personal test Instagram surface; do not upload account names, messages, contacts or private content.

## Items that require the account owner

- Sign in to the Google account that owns the existing Play Console developer account.
- Confirm the public developer/contact email shown on the listing.
- Choose the target-age groups. Do not include under-13 groups; decide explicitly whether the first release targets only adults or also teens aged 13–17.
- Complete any identity, device-verification or closed-testing requirement attached to that developer account.
- Review the final release summary immediately before production rollout.
