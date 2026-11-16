# Architecture

## Trust boundary

No Doom is a separate Android application. It never shares Instagram's UID, files, credentials or signing certificate. The official `com.instagram.android` package continues to perform all account and network activity.

The manifest contains no Internet permission. Accessibility access is package-scoped to Instagram and a short allowlist of Android system packages used to protect No Doom's own settings.

## Components

| Component | Responsibility |
|---|---|
| `MainActivity` | Guided four-step setup, separate live check and the four protection states |
| `SettingsActivity` | Notification controls and the deliberate removal flow |
| `ProtectionStateStore` | Fail-safe ON/OFF/countdown/window state using monotonic elapsed time |
| `RemovalStateStore` | Ten-minute wait and ten-minute removal window using monotonic time |
| `AlarmScheduler` | Two exact elapsed-time transitions; no polling or background ticking |
| `NoDoomAccessibilityService` | Event router; acts only while protected |
| `InstagramUiClassifier` | Centralised, version-maintainable screen recognition |
| `GestureGateOverlay` | Transparent Accessibility overlay active only in a recognised Reel viewer |
| `SelfProtectionGuard` | Narrowly guards only uninstall/device-admin removal screens |
| `RecognitionStore` | Stores timestamps and Instagram version only—never screen content |

## Reel recognition

The classifier combines:

1. Foreground package identity.
2. A vertical rail of small clickable controls on the right.
3. A large scroll surface or custom pager geometry.
4. Bottom-navigation presence and selection state, preventing inline feed Reels from being mistaken for a full-screen viewer.
5. Detection of foreground sheets so comments and share lists remain scrollable.

No single username, caption, like count or private message is used. Language tokens are limited to bottom navigation and geometry remains the fallback. The M0 device pass must calibrate these signals against the current Instagram build.

## Gesture handling

System Back/Home gesture edges are excluded from the touchable region. Within a confirmed Reel viewer:

- A clearly vertical movement is consumed.
- A tap, hold or horizontal gesture is replayed through Android's accessibility gesture API.
- The overlay disappears when comments or another foreground sheet is recognised.

Replaying a long press only after classification may not match Instagram's press timing perfectly. That is an explicit M0 go/no-go item; if it prevents R-03, the implementation must move to the documented bounce-back fallback before production.

## Lock timing

Countdown deadlines use `SystemClock.elapsedRealtime()`, which includes sleep and is unaffected by wall-clock or time-zone changes. Exact elapsed-real-time alarms open and close the window without a polling service. Reboot changes Android's boot counter; protected transitional states reset to ON, while OFF remains OFF.

Missing, corrupt or contradictory state always resolves to ON.

## Self-protection boundary

Device Administrator adds an uninstall barrier but has no lock, wipe or password policy. Settings provides a visible 10-minute removal wait followed by a 10-minute window; when ready, No Doom deactivates only its own administrator and opens Android's normal uninstall confirmation. The Accessibility guard is intentionally narrow and silent so ordinary App info, launcher and Settings screens are not closed by broad text matching.

Android still allows bypass with a computer, safe mode or factory reset. No Doom is an impulse barrier, not device-owner management software.

