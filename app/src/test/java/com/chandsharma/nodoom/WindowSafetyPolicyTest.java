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

    @Test
    public void backgroundStatusBarUpdateDoesNotSuspendInstagram() {
        assertFalse(WindowSafetyPolicy.systemUiObscures(
                "com.android.systemui",
                "com.instagram.android"));
    }

    @Test
    public void activeNotificationShadeSuspendsInstagram() {
        assertTrue(WindowSafetyPolicy.systemUiObscures(
                "com.android.systemui",
                "com.android.systemui"));
    }

    @Test
    public void closingNotificationShadeResumesProtectedApp() {
        assertTrue(WindowSafetyPolicy.systemUiReleased(
                "com.android.systemui",
                "com.instagram.android",
                true));
    }

    @Test
    public void backgroundStatusBarUpdateDoesNotInventResume() {
        assertFalse(WindowSafetyPolicy.systemUiReleased(
                "com.android.systemui",
                "com.instagram.android",
                false));
    }

    @Test
    public void overlayEventIsNotAnAppSwitch() {
        assertFalse(WindowSafetyPolicy.eventBelongsToActiveWindow(
                "com.chandsharma.nodoom",
                "com.instagram.android"));
    }

    @Test
    public void actualNoDoomWindowIsAnAppSwitch() {
        assertTrue(WindowSafetyPolicy.eventBelongsToActiveWindow(
                "com.chandsharma.nodoom",
                "com.chandsharma.nodoom"));
    }
}
