package com.chandsharma.nodoom;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public final class NoDoomAccessibilityService extends AccessibilityService
        implements GestureGateOverlay.Callback {
    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    private static final long REELS_CLICK_GRACE_MILLIS = 3_000L;
    private static final long VIEWER_EXIT_DEBOUNCE_MILLIS = 180L;
    private static final long GUARD_COOLDOWN_MILLIS = 900L;
    private static final long RECOGNITION_WRITE_COOLDOWN_MILLIS = 5_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final InstagramUiClassifier instagramClassifier = new InstagramUiClassifier();
    private final SelfProtectionGuard selfProtectionGuard = new SelfProtectionGuard();

    private ProtectionStateStore stateStore;
    private RemovalStateStore removalStateStore;
    private RecognitionStore recognitionStore;
    private GestureGateOverlay gestureGate;
    private boolean currentScreenIsReelViewer;
    private long pendingReelsTabUntilElapsed;
    private long lastViewerRecordElapsed;
    private long lastTabRecordElapsed;
    private long lastGuardElapsed;
    private int viewerExitGeneration;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        stateStore = new ProtectionStateStore(this);
        removalStateStore = new RemovalStateStore(this);
        recognitionStore = new RecognitionStore(this);
        gestureGate = new GestureGateOverlay(this, this);
        stateStore.get();
        handler.post(this::inspectCurrentWindow);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || stateStore == null) {
            return;
        }
        ProtectionSnapshot state = stateStore.get();
        if (!state.mode.isProtected()) {
            currentScreenIsReelViewer = false;
            gestureGate.hide();
            return;
        }

        CharSequence packageValue = event.getPackageName();
        String packageName = packageValue == null ? "" : packageValue.toString();
        if (INSTAGRAM_PACKAGE.equals(packageName)) {
            trackReelsNavigationClick(event);
            inspectInstagram();
            return;
        }

        currentScreenIsReelViewer = false;
        viewerExitGeneration++;
        pendingReelsTabUntilElapsed = 0L;
        gestureGate.hide();
        guardSystemScreen(packageName);
    }

    @Override
    public void onInterrupt() {
        currentScreenIsReelViewer = false;
        if (gestureGate != null) {
            gestureGate.hide();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (stateStore != null && stateStore.get().mode.isProtected() && recognitionStore != null) {
            recognitionStore.recordInterruption();
        }
        onInterrupt();
        return super.onUnbind(intent);
    }

    @Override
    public void onVerticalSwipeBlocked() {
        // Intentionally silent. A blocked swipe simply leaves the current Reel in place.
    }

    @Override
    public boolean shouldRestoreGate() {
        return currentScreenIsReelViewer
                && stateStore != null
                && stateStore.get().mode.isProtected();
    }

    private void inspectCurrentWindow() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) {
            return;
        }
        String packageName = root.getPackageName().toString();
        if (INSTAGRAM_PACKAGE.equals(packageName)) {
            inspectInstagram();
        } else {
            guardSystemScreen(packageName);
        }
    }

    private void inspectInstagram() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        Rect displayBounds = getSystemService(WindowManager.class)
                .getCurrentWindowMetrics()
                .getBounds();
        InstagramUiClassifier.Result result = instagramClassifier.classify(
                root,
                displayBounds.width(),
                displayBounds.height());
        try {
            if (result.screen == InstagramUiClassifier.Screen.REELS_TAB) {
                pendingReelsTabUntilElapsed = 0L;
                recordReelsTabIfNeeded();
                enterReelViewer(root);
                return;
            }
            if (result.screen == InstagramUiClassifier.Screen.REEL_VIEWER) {
                enterReelViewer(root);
                return;
            }
            if (result.screen == InstagramUiClassifier.Screen.REEL_MODAL) {
                viewerExitGeneration++;
                currentScreenIsReelViewer = false;
                gestureGate.hide();
                return;
            }
            if (SystemClock.elapsedRealtime() < pendingReelsTabUntilElapsed) {
                // Instagram often updates the selected tab state after the destination appears.
                // Gate immediately after the Reels click so no early swipe can slip through.
                enterReelViewer(root);
                return;
            }
            scheduleViewerExit();
        } finally {
            result.recycle();
            root.recycle();
        }
    }

    private void enterReelViewer(AccessibilityNodeInfo root) {
        viewerExitGeneration++;
        currentScreenIsReelViewer = true;
        recordViewerIfNeeded();
        Rect instagramBounds = new Rect();
        root.getBoundsInScreen(instagramBounds);
        gestureGate.show(instagramBounds);
    }

    private void scheduleViewerExit() {
        if (!currentScreenIsReelViewer) {
            gestureGate.hide();
            return;
        }
        int generation = ++viewerExitGeneration;
        handler.postDelayed(() -> {
            if (generation != viewerExitGeneration) {
                return;
            }
            currentScreenIsReelViewer = false;
            gestureGate.hide();
        }, VIEWER_EXIT_DEBOUNCE_MILLIS);
    }

    private void trackReelsNavigationClick(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return;
        }
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) {
            return;
        }
        try {
            Rect displayBounds = getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics()
                    .getBounds();
            if (!instagramClassifier.isReelsNavigationNode(source, displayBounds.height())) {
                return;
            }
            pendingReelsTabUntilElapsed = SystemClock.elapsedRealtime()
                    + REELS_CLICK_GRACE_MILLIS;
            handler.postDelayed(this::inspectCurrentWindow, 25L);
            handler.postDelayed(this::inspectCurrentWindow, 120L);
            handler.postDelayed(this::inspectCurrentWindow, 350L);
            handler.postDelayed(this::inspectCurrentWindow, 750L);
        } finally {
            source.recycle();
        }
    }

    private void recordViewerIfNeeded() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastViewerRecordElapsed < RECOGNITION_WRITE_COOLDOWN_MILLIS) {
            return;
        }
        lastViewerRecordElapsed = now;
        recognitionStore.recordViewer(instagramVersion());
    }

    private void recordReelsTabIfNeeded() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastTabRecordElapsed < RECOGNITION_WRITE_COOLDOWN_MILLIS) {
            return;
        }
        lastTabRecordElapsed = now;
        recognitionStore.recordReelsTab(instagramVersion());
    }

    private void guardSystemScreen(String packageName) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastGuardElapsed < GUARD_COOLDOWN_MILLIS) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        try {
            boolean removalAllowed = removalStateStore != null
                    && removalStateStore.removalAllowed();
            if (!selfProtectionGuard.shouldClose(packageName, root, removalAllowed)) {
                return;
            }
        } finally {
            if (root != null) {
                root.recycle();
            }
        }
        lastGuardElapsed = now;
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private String instagramVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(INSTAGRAM_PACKAGE, 0);
            return info.versionName == null
                    ? String.valueOf(info.getLongVersionCode())
                    : info.versionName;
        } catch (PackageManager.NameNotFoundException notInstalled) {
            return "unknown";
        }
    }
}

