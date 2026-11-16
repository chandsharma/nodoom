package com.chandsharma.nodoom;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/** Instagram-specific recognition. No service lifecycle or overlay code belongs here. */
final class InstagramProtectionModule implements ProtectedAppModule {
    static final String PACKAGE_NAME = "com.instagram.android";
    static final String COMMENTS_SCROLL_VIEW_ID = PACKAGE_NAME + ":id/sticky_header_list";
    static final String SHARE_SCROLL_VIEW_ID =
            PACKAGE_NAME + ":id/direct_private_share_recipients_recycler_view";
    static final String COLLECTION_SCROLL_VIEW_ID = PACKAGE_NAME + ":id/recycler_view";
    static final String BOTTOM_SHEET_CONTAINER_VIEW_ID =
            PACKAGE_NAME + ":id/bottom_sheet_container";

    private static final String[] MODAL_SCROLL_VIEW_IDS = {
            SHARE_SCROLL_VIEW_ID,
            COMMENTS_SCROLL_VIEW_ID
    };

    private final InstagramUiClassifier classifier = new InstagramUiClassifier();

    @Override
    public String packageName() {
        return PACKAGE_NAME;
    }

    @Override
    public String displayName() {
        return "Instagram";
    }

    @Override
    public Detection inspect(AccessibilityNodeInfo root, int screenWidth, int screenHeight) {
        InstagramUiClassifier.Result result = classifier.classify(
                root,
                screenWidth,
                screenHeight);
        Surface surface;
        switch (result.screen) {
            case REELS_TAB:
                surface = Surface.INFINITE_FEED;
                break;
            case REEL_VIEWER:
                surface = Surface.SINGLE_ITEM_VIEWER;
                break;
            case REEL_MODAL:
                surface = Surface.MODAL;
                break;
            case ORDINARY:
                surface = Surface.ORDINARY;
                break;
            default:
                surface = Surface.UNKNOWN;
                break;
        }
        return new Detection(
                surface,
                result.strongViewerSignature,
                result.otherNavigationSelected,
                result.bottomNavigationTop);
    }

    @Override
    public NavigationTarget navigationTarget(AccessibilityNodeInfo node, int screenHeight) {
        InstagramUiClassifier.NavigationTarget target = classifier.navigationTarget(
                node,
                screenHeight);
        switch (target) {
            case REELS:
                return NavigationTarget.INFINITE_FEED;
            case OTHER:
                return NavigationTarget.OTHER;
            default:
                return NavigationTarget.NONE;
        }
    }

    @Override
    public boolean opensModal(CharSequence text, CharSequence contentDescription) {
        return ReelControlPolicy.opensModal(text, contentDescription);
    }

    @Override
    public boolean performModalScroll(AccessibilityNodeInfo root, int action) {
        for (String viewId : MODAL_SCROLL_VIEW_IDS) {
            if (performScrollByViewId(root, viewId, action)) {
                return true;
            }
        }
        // Instagram reuses the generic recycler_view ID elsewhere. Search for it only inside a
        // visible collection bottom sheet, never from the root where the Reel pager could match.
        List<AccessibilityNodeInfo> sheets =
                root.findAccessibilityNodeInfosByViewId(BOTTOM_SHEET_CONTAINER_VIEW_ID);
        try {
            for (int index = sheets.size() - 1; index >= 0; index--) {
                AccessibilityNodeInfo sheet = sheets.get(index);
                if (sheet.isVisibleToUser()
                        && performScrollByViewId(sheet, COLLECTION_SCROLL_VIEW_ID, action)) {
                    return true;
                }
            }
        } finally {
            recycleAll(sheets);
        }
        return false;
    }

    private boolean performScrollByViewId(
            AccessibilityNodeInfo root,
            String viewId,
            int action) {
        List<AccessibilityNodeInfo> candidates = root.findAccessibilityNodeInfosByViewId(viewId);
        try {
            for (int index = candidates.size() - 1; index >= 0; index--) {
                AccessibilityNodeInfo candidate = candidates.get(index);
                if (candidate.isVisibleToUser() && candidate.performAction(action)) {
                    return true;
                }
            }
            return false;
        } finally {
            recycleAll(candidates);
        }
    }

    private void recycleAll(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo node : nodes) {
            node.recycle();
        }
    }
}
