# Privacy statement

No Doom is designed so that it cannot send data to its developer:

- It requests no Internet permission.
- It contains no analytics, ads, telemetry, crash reporter or update checker.
- It has no account and does not ask for an Instagram password.
- It does not use Instagram or Meta APIs.
- It does not read Instagram files, cookies or tokens.
- It does not store accessibility text, usernames, captions, messages, images or videos.
- Android cloud backup and device-transfer backup are disabled.

The Accessibility service processes the live UI structure of:

1. The official Instagram package, to distinguish a Reel viewer, Reels tab and foreground comment/share sheets.
2. Android System UI, only to detect that the notification shade, Quick Settings or another system surface is in front and immediately release No Doom's transparent gesture layer.
3. Android Settings and Permission Controller, only to recognise No Doom's exact device-admin removal or uninstall confirmation while its removal timer is not ready.

Persisted data is limited to protection/removal state, elapsed-time deadlines, notification and optional-toast choices, setup completion, last recognition timestamps, Instagram version and an interruption timestamp. Clearing app data removes it; missing protection data fails safe to protection ON.

