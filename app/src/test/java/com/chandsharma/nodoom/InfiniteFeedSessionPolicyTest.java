package com.chandsharma.nodoom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InfiniteFeedSessionPolicyTest {
    @Test
    public void oldHomeTreeDuringTabTransitionDoesNotCancelSession() {
        assertFalse(InfiniteFeedSessionPolicy.shouldClear(
                true, 25L, 1_000L, true, true, false));
    }

    @Test
    public void settledHomeNavigationCancelsSession() {
        assertTrue(InfiniteFeedSessionPolicy.shouldClear(
                true, 1_001L, 1_000L, true, true, false));
    }

    @Test
    public void strongViewerDoesNotCancelSession() {
        assertFalse(InfiniteFeedSessionPolicy.shouldClear(
                true, 1_001L, 1_000L, false, false, true));
    }
}
