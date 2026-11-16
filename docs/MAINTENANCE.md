# Instagram compatibility maintenance

Instagram changes frequently. Recognition logic is intentionally concentrated in `InstagramUiClassifier.java`.

## After an Instagram update

1. Record the Instagram version from No Doom's health check.
2. Run the live check before ordinary use.
3. Exercise every entry point listed in the product requirements.
4. Run the unaffected-screen checks for Home, DMs, stories, Explore, profiles and comments.
5. If recognition changed, capture only a redacted diagnostic description. Never publish raw DMs, captions, usernames, account IDs or session data.
6. Update the classifier using geometry, roles, resource IDs and selected state before adding translated visible text.
7. Run `./gradlew test lint assembleDebug`.
8. Install the candidate over the existing build and complete the full affected checklist.

## Recognition change rules

- Do not scatter Instagram assumptions through the service.
- Require multiple independent signals for a Reel viewer.
- Treat false positives outside Reels as release blockers.
- A new Instagram version is not marked compatible until it has passed on a physical Pixel.
- Never weaken protection silently. Document any last-resort close-viewer behaviour.

## Diagnostic design

The planned diagnostic export stores only:

- Instagram version.
- Android version and display metrics.
- Node class, view ID, bounds, click/scroll/selected booleans.
- Stable hashes of labels if a label role must be compared.

It must omit raw text and descriptions. Export is owner-initiated and stays local until manually shared. This feature is not enabled in `0.1.0-alpha.1` because its privacy review is not complete.

