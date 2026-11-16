package com.chandsharma.nodoom;

/** Pure fail-closed rule for deciding whether a gesture overlay may exist. */
final class WindowSafetyPolicy {
    private WindowSafetyPolicy() {
    }

    static boolean canGate(
            String activePackage,
            String protectedPackage,
            boolean systemUiObscuring,
            boolean protectedViewer,
            boolean protectionEnabled) {
        return protectionEnabled
                && protectedViewer
                && !systemUiObscuring
                && protectedPackage != null
                && protectedPackage.equals(activePackage);
    }
}
