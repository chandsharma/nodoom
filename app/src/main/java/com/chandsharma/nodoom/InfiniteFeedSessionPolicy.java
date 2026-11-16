package com.chandsharma.nodoom;

/** Pure transition rule that keeps a tab click alive while Instagram replaces its old window. */
final class InfiniteFeedSessionPolicy {
    private InfiniteFeedSessionPolicy() {
    }

    static boolean shouldClear(
            boolean sessionActive,
            long sessionAgeMillis,
            long transitionGraceMillis,
            boolean otherNavigationSelected,
            boolean ordinarySurface,
            boolean strongViewerSignature) {
        if (!sessionActive || sessionAgeMillis <= transitionGraceMillis) {
            return false;
        }
        return otherNavigationSelected || (ordinarySurface && !strongViewerSignature);
    }
}
