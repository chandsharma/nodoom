# M0 feasibility report

Status: **0.1 failed initial Pixel validation; 0.2 awaits focused retest**

An APK compiling successfully is not proof that another app's current accessibility tree and touch behaviour meet the product requirements. M0 remains a hard gate.

## Environment

| Item | Value |
|---|---|
| Date | 16 Nov 2026 initial test |
| Device | Pixel 9 Pro |
| Android version/build | Not recorded |
| Navigation mode | Not recorded |
| Instagram version | 454.0.0.54.77 |
| No Doom version | 0.1.0-alpha.1 tested; 0.2.0-alpha.2 pending |

## Candidate A: suppress the swipe

Implementation: a transparent Accessibility overlay consumes a clearly vertical gesture and replays non-vertical gestures.

Expected advantage: the next Reel is never requested and the starting Reel keeps playing.

Risk to validate: tap-and-hold is replayed after gesture classification, which may not preserve Instagram's exact pause timing. Multi-touch and UI controls must also pass R-03.

Initial result on 0.1: **Fail**. Inline Home Reels sometimes matched the viewer signature, while genuine full-screen/Reels-tab screens were recognised too late or inconsistently. Version 0.2 makes bottom-navigation presence a strong feed/profile signal, gates immediately after a Reels-tab click, debounces transient tree changes and removes all block toasts. Screen recording: Not captured.

## Candidate B: bounce back

Implementation: not enabled. It will be implemented only if Candidate A cannot pass R-03, because it can expose or restart media and is lower in the owner's preference order.

Result: Not tested. Screen recording: Not captured.

## Entry-point matrix

Home and profile entry points were sampled on 0.1 but failed consistency requirements. All entry points must be rerun on 0.2: Home, one-to-one DM, group DM, story share, Explore, own/other profile, search, audio, hashtag, saved collection, notification, external link and a Reel opened from another Reel context.

## Go/no-go decision

Pending owner review of device evidence. No production release should be labelled stable before this report names the selected behaviour and records R-03 compatibility.

