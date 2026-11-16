package com.chandsharma.nodoom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SelfProtectionGuardTest {
    private final SelfProtectionGuard guard = new SelfProtectionGuard();

    @Test
    public void onlyRemovalSystemPackagesMayBeInspected() {
        assertTrue(guard.handlesPackage("com.android.settings"));
        assertTrue(guard.handlesPackage("com.google.android.permissioncontroller"));
        assertFalse(guard.handlesPackage("com.instagram.android"));
        assertFalse(guard.handlesPackage("com.example.private.app"));
        assertFalse(guard.handlesPackage(null));
    }
}
