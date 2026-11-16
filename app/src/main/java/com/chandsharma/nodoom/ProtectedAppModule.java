package com.chandsharma.nodoom;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Adapter between the generic gesture guard and one supported social app.
 *
 * <p>Future YouTube Shorts or X modules belong behind this interface. The accessibility service
 * owns window safety and gesture forwarding; a module owns only app-specific UI recognition.</p>
 */
interface ProtectedAppModule {
    enum Surface {
        INFINITE_FEED,
        SINGLE_ITEM_VIEWER,
        MODAL,
        ORDINARY,
        UNKNOWN
    }

    enum NavigationTarget {
        INFINITE_FEED,
        OTHER,
        NONE
    }

    final class Detection {
        final Surface surface;
        final boolean strongViewerSignature;
        final boolean otherNavigationSelected;

        Detection(
                Surface surface,
                boolean strongViewerSignature,
                boolean otherNavigationSelected) {
            this.surface = surface;
            this.strongViewerSignature = strongViewerSignature;
            this.otherNavigationSelected = otherNavigationSelected;
        }
    }

    String packageName();

    String displayName();

    Detection inspect(AccessibilityNodeInfo root, int screenWidth, int screenHeight);

    NavigationTarget navigationTarget(AccessibilityNodeInfo node, int screenHeight);
}
