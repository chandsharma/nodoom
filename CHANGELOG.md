# Changelog

All notable user-facing changes are recorded here. Alpha releases remain subject to the physical-device acceptance report in [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md).

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
