package com.chandsharma.nodoom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WindowSafetyPolicyTest {
    @Test
    public void notificationShadeAlwaysReleasesGate() {
        assertFalse(WindowSafetyPolicy.canGate(
                "com.instagram.android",
                "com.instagram.android",
                true,
                true,
                true));
    }

    @Test
    public void anotherActiveWindowAlwaysReleasesGate() {
        assertFalse(WindowSafetyPolicy.canGate(
                "com.android.systemui",
                "com.instagram.android",
                false,
                true,
                true));
    }

    @Test
    public void matchingProtectedViewerCanGate() {
        assertTrue(WindowSafetyPolicy.canGate(
                "com.instagram.android",
                "com.instagram.android",
                false,
                true,
                true));
    }
}
