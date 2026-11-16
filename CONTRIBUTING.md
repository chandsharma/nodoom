# Contributing to No Doom

Thank you for helping make intentional social-media use possible.

## Before you start

1. Search existing issues and the roadmap.
2. For recognition changes, state the Android version, Instagram version, device and entry point.
3. Never attach an Instagram APK, session cookie, login token, private message, username, caption, screenshot containing personal data, or raw accessibility dump.
4. Open an issue before adding a permission, dependency, background component, network capability or supported app.

## Development rules

- Keep the official Instagram app unmodified.
- Never add `android.permission.INTERNET` or a networking/analytics/crash-reporting SDK.
- Never automate a like, follow, comment, share, message, view or account action.
- Fail conservatively. A screen that cannot be confidently classified must not affect ordinary Instagram scrolling.
- Recognition changes belong in `InstagramUiClassifier` and need a false-positive test plan.
- Release builds must not log or persist accessibility content.
- Keep the installed app below 10 MB and avoid polling/background loops.

## Pull requests

Run:

```bash
./gradlew test lint assembleDebug
```

Include:

- The requirement or issue addressed.
- What changed and why.
- Device/Android/Instagram versions tested.
- Results for affected checks in `docs/TEST_REPORT.md`.
- A privacy and permission impact statement.

By contributing, you agree that your contribution is licensed under Apache-2.0.

