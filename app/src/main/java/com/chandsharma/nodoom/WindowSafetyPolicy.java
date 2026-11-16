package com.chandsharma.nodoom;

/** Pure fail-closed rule for deciding whether a gesture overlay may exist. */
final class WindowSafetyPolicy {
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";

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

    static boolean systemUiObscures(String eventPackage, String activePackage) {
        return SYSTEM_UI_PACKAGE.equals(eventPackage)
                && SYSTEM_UI_PACKAGE.equals(activePackage);
    }

    static boolean systemUiReleased(
            String eventPackage,
            String activePackage,
            boolean wasObscuring) {
        return wasObscuring
                && SYSTEM_UI_PACKAGE.equals(eventPackage)
                && activePackage != null
                && !activePackage.isEmpty()
                && !SYSTEM_UI_PACKAGE.equals(activePackage);
    }

    static boolean eventBelongsToActiveWindow(String eventPackage, String activePackage) {
        return eventPackage != null
                && !eventPackage.isEmpty()
                && eventPackage.equals(activePackage);
    }
}
