# Changelog

All notable user-facing changes are recorded here. Alpha releases remain subject to the physical-device acceptance report in [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md).

## 0.3.0-alpha.3 — 2026-11-16

### Changed

- Made routine protection completely quiet by default, with optional settings for an always-on status notification and a rate-limited blocked-swipe hint.
- Split app-specific recognition behind a `ProtectedAppModule` interface so future platforms can be added without changing the window-safety and gesture core.
- Made the Reels-tab session survive transient Instagram accessibility-tree changes while still releasing immediately on ordinary navigation.

### Fixed

- Keeps the gesture overlay out of the Android status-bar strip so opening the notification shade always starts as a native System UI gesture.
- Listens for Android System UI windows and immediately removes the Reel gate while the notification shade, Quick Settings or another System UI surface is in front.
- Rechecks the active foreground package before restoring the gate after a forwarded tap.
- Releases the gate immediately on a confirmed Home, Explore or profile surface instead of retaining it through an exit debounce.
- Gives foreground comments and share sheets priority over the selected Reels tab so their lists remain native and scrollable.
- Requires two consecutive observations and two independent geometry signals before an unselected full-screen viewer is gated, reducing intermittent Home-feed false positives.

### Known limitations

- Physical retesting on the owner's Pixel 9 Pro is required before this build can be considered reliable.
- Accessibility-based recognition remains sensitive to Instagram UI changes; the in-app live check is mandatory after Instagram updates.

## 0.2.0-alpha.2 — 2026-11-16

### Changed

- Reworked first-run setup into four numbered Android-access steps.
- Split device access from Instagram live-test evidence on the main screen.
- Added a dedicated Settings screen for notification choices and app removal.
- Changed the Reels tab from a delayed redirect to the same one-Reel rule used by other full-screen viewers.
- Added a clear 10-minute wait and 10-minute window before deactivating the uninstall barrier.
- Replaced the placeholder launcher icon with the No Doom broken-infinity mind mark.

### Fixed

- Prevented inline Home-feed Reels from activating the full-screen gesture gate merely because they expose a right-side action rail.
- Starts gesture protection immediately after a Reels-tab click instead of waiting for Instagram's selected-state update.
- Debounces transient Accessibility-tree changes that previously let swipes slip through.
- Restores the gesture gate immediately after forwarding a tap or horizontal gesture.
- Removed all blocked-swipe and Reels-tab toasts.
- Removed the “No Doom is protecting you” notification and its legacy high-priority channel.
- Narrowed self-protection matching so opening the launcher, No Doom or ordinary Settings pages cannot close the app.

### Known limitations

- Physical retesting on the owner's Pixel 9 Pro and Instagram `454.0.0.54.77` is still required.
- Google Play Protect may block browser/file-manager installation of a sideloaded Accessibility app while the appeal is pending. ADB remains the temporary owner-testing path.
- Instagram can change its private UI structure at any time; the in-app live check must pass after Instagram updates.

## 0.1.0-alpha.1 — 2026-11-16

- Initial owner-signed feasibility release.
- Failed the first physical-device pass because Reel recognition was inconsistent, inline feed content could be falsely gated, self-protection was overbroad, and block feedback was noisy.
