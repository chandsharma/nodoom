# Architecture

## Trust boundary

No Doom is a separate Android application. It never shares Instagram's UID, files, credentials or signing certificate. The official `com.instagram.android` package continues to perform all account and network activity.

The manifest contains no Internet permission. Android must deliver foreground package transitions globally so No Doom can remove its transparent gate immediately when another app appears. The service filters before reading a window: it inspects UI structure only for Instagram, Android System UI, Android Settings and Permission Controller. For every other package it observes the package name, clears any gate and returns without requesting or traversing that app's accessibility tree.

## Components

| Component | Responsibility |
|---|---|
| `MainActivity` | Guided four-step setup, separate live check and the four protection states |
| `SettingsActivity` | Notification controls and the deliberate removal flow |
| `ProtectionStateStore` | Fail-safe ON/OFF/countdown/window state using monotonic elapsed time |
| `RemovalStateStore` | Ten-minute wait and ten-minute removal window using monotonic time |
| `AlarmScheduler` | Two exact elapsed-time transitions; no polling or background ticking |
| `NoDoomAccessibilityService` | Generic window-safety and gesture-session coordinator; acts only while protected |
| `ProtectedAppModule` / `ProtectedAppRegistry` | Stable extension boundary for app-specific recognition, modal controls and list scrolling |
| `InstagramProtectionModule` | Maps Instagram recognition and known modal list IDs into generic protected surfaces |
| `InstagramUiClassifier` | Centralised, version-maintainable screen recognition |
| `GestureGateOverlay` | Transparent Accessibility overlay active only in a recognised Reel viewer |
| `WindowSafetyPolicy` | Fail-closed foreground-package and System UI rule |
| `SelfProtectionGuard` | Narrowly guards only uninstall/device-admin removal screens |
| `RecognitionStore` | Stores timestamps and Instagram version only—never screen content |

## Reel recognition

The classifier combines:

1. Foreground package identity.
2. A vertical rail of small clickable controls on the right.
3. A large scroll surface or custom pager geometry.
4. Exact Instagram bottom-tab IDs, bottom-screen geometry and descendant selection state, preventing inline Home Reels and nearby feed controls from being mistaken for a full-screen viewer.
5. Exact visible markers for comments, sharing and the saved-collection sheet, plus conservative geometry for other scrollable sheets.
6. Two consecutive observations for an unselected viewer, preventing a single incomplete Home tree from enabling the gate.

No single username, caption, like count or private message is used. Language tokens are limited to bottom navigation and geometry remains the fallback. The M0 device pass must calibrate these signals against the current Instagram build.

## Gesture handling

The status-bar and display-cutout strip is excluded from the touchable region. Android System UI events immediately remove the overlay, and the active foreground package is checked again before a forwarded gesture can restore it. Within a confirmed Reel viewer:

- A clearly vertical movement is consumed.
- A tap, hold or horizontal gesture is replayed through Android's accessibility gesture API after the intercepted touch finishes.
- Recognised comment/share controls use a direct native action while the gate remains attached across the transition.
- A vertical gesture inside a recognised comments, sharing or collection list is consumed and converted into a scroll action on that exact foreground list. Unknown sheets stay fail-closed and cannot pass a gesture to the Reel pager underneath.
- While a foreground sheet is open, a short-lived modal watcher retains the protected Reel session and rearms ordinary viewer classification as soon as the sheet closes, including transitions where Instagram omits a close event.
- Home, Explore and profile surfaces release the overlay immediately.
- While a viewer is protected, a rate-limited check reads only the exact bottom-tab subtree. If Instagram returns to Home without a normal window event, this lightweight escape path removes the stale gate without scanning the large video tree.

Because a long press is replayed after classification, its effect begins after the finger is released rather than during the original hold. Instagram's edge-hold 2× playback interaction is functional on the tested build, but this timing difference and multi-touch remain explicit alpha limitations. If either prevents R-03 in daily use, the implementation must move to the documented bounce-back fallback before a stable release.

## Lock timing

Countdown deadlines use `SystemClock.elapsedRealtime()`, which includes sleep and is unaffected by wall-clock or time-zone changes. Exact elapsed-real-time alarms open and close the window without a polling service. Reboot changes Android's boot counter; protected transitional states reset to ON, while OFF remains OFF.

Missing, corrupt or contradictory state always resolves to ON.

## Self-protection boundary

Device Administrator adds an uninstall barrier but has no lock, wipe or password policy. Settings provides a visible 10-minute removal wait followed by a 10-minute window; when ready, No Doom deactivates only its own administrator and opens Android's normal uninstall confirmation. The Accessibility guard is intentionally narrow and silent so ordinary App info, launcher and Settings screens are not closed by broad text matching.

Android still allows bypass with a computer, safe mode or factory reset. No Doom is an impulse barrier, not device-owner management software.

