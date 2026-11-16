# Changelog

All notable user-facing changes are recorded here. Alpha releases remain subject to the physical-device acceptance report in [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md).

## 0.5.0-alpha.5 — 2026-11-16

### Fixed

- Opens comments and sharing with their native accessibility actions, then scrolls only Instagram's exact comments/share list while the underlying Reel gate stays attached.
- Recognises Instagram's saved-collection sheet and scrolls its collection list without exposing the Reel underneath.
- Keeps the protected Reel session behind a foreground sheet and watches its close transition, preventing one or two next-Reel swipes from slipping through after comments, sharing or save UI closes.
- Preserves native like/unlike and save/unsave controls with a short, reliable gesture-replay hand-off instead of an Accessibility click that Instagram can ignore.
- Replays edge holds for Instagram's 2× playback interaction, then independently reinspects the viewer so protection cannot remain disarmed when the held-playback surface emits no final event.
- Classifies the final finger displacement even when Android supplies only DOWN/UP events, closing the short-swipe leak seen after dismissing a sheet.
- Coalesces Instagram content changes and never traverses a large modal tree from touch dispatch, preventing the gesture-window timeout seen during early testing.

### Validation

- Passed 33 unit tests and Android lint with zero issues.
- The owner-signed candidate passed focused Pixel 9 Pro checks for five blocked Reel swipes, comment/share/collection scrolling, immediate post-modal rearming, like/unlike, save/unsave and edge-hold 2× playback.
- Home, native bottom navigation, foreground switching and notification-shade isolation remain covered by the physical regression; the broader entry-point matrix and extended daily-use testing remain open.

## 0.4.0-alpha.4 — 2026-11-16

### Fixed

- Restricts bottom-navigation recognition to Instagram's actual lowest navigation band, preventing lower Home-feed controls from hiding the Home signal and activating the Reel gate.
- Receives foreground-package transitions for every app so the transparent gate is removed immediately when leaving Instagram.
- Preserves the privacy boundary: for unrelated apps, No Doom reads only the package name from the transition event and never requests or traverses their accessibility tree.
- Limits removal-screen tree inspection to Android Settings and Permission Controller before obtaining the root window.
- Distinguishes an active notification shade from background status-bar updates, so routine System UI events cannot silently remove Reel protection.
- Ignores No Doom's own background overlay-window event unless the No Doom app is actually foreground, preventing the gate from removing itself immediately after attachment.
- Leaves Instagram's detected bottom-navigation strip completely outside the overlay, so Home, Search, messages and Profile taps remain native instead of depending on gesture replay.
- Activates accessible Reel controls directly, with gesture replay only as a fallback, so comments and sharing open reliably while vertical paging stays blocked.
- Restores Reel protection from the System UI close transition itself, including when Instagram emits no fresh event after the notification shade closes.

### Validation

- Passed 25 unit tests and Android lint with zero issues.
- The owner-signed APK passed a focused Pixel 9 Pro / Android 17 regression against official Instagram `454.0.0.54.77`: Home scroll, five blocked Reel swipes, native bottom navigation, comments and share-sheet scrolling, foreground switching, and notification-shade open/close with immediate gate restoration.
- The broader entry-point matrix and extended battery/daily-use testing remain open before a stable release.

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
