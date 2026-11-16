# Acceptance test report

Status: **No physical-device tests have been claimed.**

| Field | Value |
|---|---|
| Device | Pixel 9 Pro (pending connection) |
| Android version/build | Pending |
| Instagram version | Pending |
| No Doom version | 0.1.0-alpha.1 |
| Test date | Pending |

The authoritative acceptance checklist is in `PRODUCT_REQUIREMENTS.md`. Results must be recorded as **Pass**, **Fail** or **Not run** with evidence; empty checkboxes must never be interpreted as a pass.

## Automated checks

| Check | Result |
|---|---|
| Unit tests | Pass — 16 Nov 2026 |
| Android lint | Pass, zero issues — 16 Nov 2026 |
| Debug APK | Pass — 80,589 bytes, `6ff0cbb8e4cbf9dab8331e5723b39a5b112fdc1a396ae44e1ae98121ae70f82f` |
| Release APK signature | No owner key created yet |
| Manifest permission audit | Pass — only notifications, boot recovery and exact alarms; no Internet permission |

## Physical acceptance

T-01 through T-54: **Not run**.

## Performance measurements

| Measurement | Result |
|---|---|
| APK size | Debug: 80,589 bytes; signed/minified release pending |
| Idle memory | Pending |
| Two-hour Instagram battery impact | Pending |
| Overnight wake-ups | Pending |

