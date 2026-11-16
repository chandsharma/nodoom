package com.chandsharma.nodoom;

import android.view.accessibility.AccessibilityNodeInfo;

/** Instagram-specific recognition. No service lifecycle or overlay code belongs here. */
final class InstagramProtectionModule implements ProtectedAppModule {
    static final String PACKAGE_NAME = "com.instagram.android";

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
                result.otherNavigationSelected);
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
}
