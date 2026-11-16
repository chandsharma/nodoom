# M0 feasibility report

Status: **0.5 passed the focused Pixel regression; full M0 entry-point coverage remains open**

An APK compiling successfully is not proof that another app's current accessibility tree and touch behaviour meet the product requirements. M0 remains a hard gate.

## Environment

| Item | Value |
|---|---|
| Date | 16 Nov 2026 initial test |
| Device | Pixel 9 Pro |
| Android version/build | Android 17 / API 37; `google/caiman/caiman:17/CP3A.261005.005/16271449:user/release-keys` |
| Navigation mode | Not recorded |
| Instagram version | 454.0.0.54.77 |
| No Doom version | 0.1 through 0.5.0-alpha.5 tested |

## Candidate A: suppress the swipe

Implementation: a transparent Accessibility overlay consumes a clearly vertical gesture, activates accessible controls directly, and replays other non-vertical gestures only as a fallback.

Expected advantage: the next Reel is never requested and the starting Reel keeps playing.

Risk to validate: tap-and-hold is replayed after gesture classification, which may not preserve Instagram's exact pause timing. Multi-touch and UI controls must also pass R-03.

Focused 0.5 result: **Pass**. Five vertical Reel swipes were consumed without advancing. Comments, sharing and the saved-collection list scrolled on their first gesture; immediate short swipes after closing stayed on the chosen Reel; and like/unlike, save/unsave and edge-hold 2× playback remained usable. Native bottom navigation, Home scrolling and notification-shade open/close also remain covered. Earlier 0.1–0.4 findings are documented in the test report. Local screenshots were used as evidence but are not published because they contain third-party and account content.

## Candidate B: bounce back

Implementation: not enabled. It will be implemented only if Candidate A cannot pass R-03, because it can expose or restart media and is lower in the owner's preference order.

Result: Not tested. Screen recording: Not captured.

## Entry-point matrix

The focused 0.5 pass covered Home, the selected Reels tab, comments, sharing, like, save, edge-hold 2× playback, notification-shade open/close and switching to No Doom. The full matrix still requires Home Reel, one-to-one DM, group DM, story share, Explore, own/other profile, search, audio, hashtag, saved collection, notification, external link and a Reel opened from another Reel context.

## Go/no-go decision

Candidate A is selected for the public alpha. No release should be labelled stable until the full entry-point matrix, long-press/multi-touch compatibility, battery measurements and extended daily-use run are complete.

