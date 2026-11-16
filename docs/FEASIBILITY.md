# M0 feasibility report

Status: **Awaiting physical Pixel 9 Pro validation**

An APK compiling successfully is not proof that another app's current accessibility tree and touch behaviour meet the product requirements. M0 remains a hard gate.

## Environment

| Item | Value |
|---|---|
| Date | Not tested |
| Device | Pixel 9 Pro |
| Android version/build | Not recorded |
| Navigation mode | Not recorded |
| Instagram version | Not recorded |
| No Doom version | 0.1.0-alpha.1 |

## Candidate A: suppress the swipe

Implementation: a transparent Accessibility overlay consumes a clearly vertical gesture and replays non-vertical gestures.

Expected advantage: the next Reel is never requested and the starting Reel keeps playing.

Risk to validate: tap-and-hold is replayed after gesture classification, which may not preserve Instagram's exact pause timing. Multi-touch and UI controls must also pass R-03.

Result: Not tested. Screen recording: Not captured.

## Candidate B: bounce back

Implementation: not enabled. It will be implemented only if Candidate A cannot pass R-03, because it can expose or restart media and is lower in the owner's preference order.

Result: Not tested. Screen recording: Not captured.

## Entry-point matrix

All entry points remain **not tested** until observed on the physical phone: Home, one-to-one DM, group DM, story share, Explore, own/other profile, search, audio, hashtag, saved collection, notification, external link and a Reel opened from another Reel context.

## Go/no-go decision

Pending owner review of device evidence. No production release should be labelled stable before this report names the selected behaviour and records R-03 compatibility.

