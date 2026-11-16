package com.chandsharma.nodoom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ReelControlPolicyTest {
    @Test
    public void commentsOpenModal() {
        assertTrue(ReelControlPolicy.opensModal(null, "Comment"));
        assertTrue(ReelControlPolicy.opensModal(
                "Comment number is 21. View comments",
                null));
    }

    @Test
    public void sharingOpensModal() {
        assertTrue(ReelControlPolicy.opensModal(null, "Share"));
    }

    @Test
    public void likeAndSaveUseNativeGestureReplay() {
        assertFalse(ReelControlPolicy.opensModal(null, "Like"));
        assertFalse(ReelControlPolicy.opensModal(null, "Save"));
    }

    @Test
    public void modalReturnRestoresProtectedViewerAcrossTransientOrdinaryState() {
        assertTrue(ReelControlPolicy.shouldRestoreAfterModal(true, false));
    }

    @Test
    public void modalReturnDoesNotOverrideConfirmedNavigation() {
        assertFalse(ReelControlPolicy.shouldRestoreAfterModal(true, true));
        assertFalse(ReelControlPolicy.shouldRestoreAfterModal(false, false));
    }

    @Test
    public void modalReturnGuardSurvivesRepeatedTransientFrames() {
        assertTrue(ReelControlPolicy.shouldProtectDuringModalReturn(false, 1_500L, 2_000L));
        assertFalse(ReelControlPolicy.shouldProtectDuringModalReturn(false, 2_000L, 2_000L));
        assertFalse(ReelControlPolicy.shouldProtectDuringModalReturn(true, 1_500L, 2_000L));
    }
}
