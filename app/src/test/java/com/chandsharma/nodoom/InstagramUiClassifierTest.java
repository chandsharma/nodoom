package com.chandsharma.nodoom;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class InstagramUiClassifierTest {
    @Test
    public void inlineReelWithBottomNavigationStaysScrollable() {
        assertEquals(
                InstagramUiClassifier.Screen.ORDINARY,
                InstagramUiClassifier.decide(false, true, 5, true, false));
    }

    @Test
    public void selectedReelsTabIsAlwaysRecognised() {
        assertEquals(
                InstagramUiClassifier.Screen.REELS_TAB,
                InstagramUiClassifier.decide(true, true, 4, true, false));
    }

    @Test
    public void fullScreenViewerWithoutBottomNavigationIsProtected() {
        assertEquals(
                InstagramUiClassifier.Screen.REEL_VIEWER,
                InstagramUiClassifier.decide(false, false, 4, true, false));
    }

    @Test
    public void foregroundCommentsSheetReleasesGestureGate() {
        assertEquals(
                InstagramUiClassifier.Screen.REEL_MODAL,
                InstagramUiClassifier.decide(false, false, 4, true, true));
    }

    @Test
    public void foregroundSheetWinsOverSelectedReelsTab() {
        assertEquals(
                InstagramUiClassifier.Screen.REEL_MODAL,
                InstagramUiClassifier.decide(true, true, 4, true, true));
    }

    @Test
    public void ordinaryFullScreenPageWithoutActionRailIsUntouched() {
        assertEquals(
                InstagramUiClassifier.Screen.OTHER,
                InstagramUiClassifier.decide(false, false, 2, true, false));
    }

    @Test
    public void actionRailWithoutFullscreenPagerIsUntouched() {
        assertEquals(
                InstagramUiClassifier.Screen.OTHER,
                InstagramUiClassifier.decide(false, false, 5, false, false));
    }

    @Test
    public void pixelBottomNavigationBandIsIncluded() {
        org.junit.Assert.assertTrue(InstagramUiClassifier.isBottomNavigationCandidate(
                2034, 192, 108, 960, 2142));
    }

    @Test
    public void lowerFeedControlsAreNotNavigationCandidates() {
        org.junit.Assert.assertFalse(InstagramUiClassifier.isBottomNavigationCandidate(
                1800, 120, 100, 960, 2142));
    }
}
