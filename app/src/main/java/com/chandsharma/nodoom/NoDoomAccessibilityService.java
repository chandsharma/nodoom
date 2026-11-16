package com.chandsharma.nodoom;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public final class NoDoomAccessibilityService extends AccessibilityService
        implements GestureGateOverlay.Callback {
    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    private static final long HINT_COOLDOWN_MILLIS = 4_000L;
    private static final long TAB_ACTION_COOLDOWN_MILLIS = 900L;
    private static final long GUARD_COOLDOWN_MILLIS = 1_500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final InstagramUiClassifier instagramClassifier = new InstagramUiClassifier();
    private final SelfProtectionGuard selfProtectionGuard = new SelfProtectionGuard();

    private ProtectionStateStore stateStore;
    private RecognitionStore recognitionStore;
    private NotificationController notifications;
    private GestureGateOverlay gestureGate;
    private BlockCoverOverlay blockCover;
    private boolean currentScreenIsReelViewer;
    private long lastHintElapsed;
    private long lastTabActionElapsed;
    private long lastGuardElapsed;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        stateStore = new ProtectionStateStore(this);
        recognitionStore = new RecognitionStore(this);
        notifications = new NotificationController(this);
        gestureGate = new GestureGateOverlay(this, this);
        blockCover = new BlockCoverOverlay(this);
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
            blockCover.hide();
            return;
        }

        CharSequence packageValue = event.getPackageName();
        String packageName = packageValue == null ? "" : packageValue.toString();
        if (INSTAGRAM_PACKAGE.equals(packageName)) {
            inspectInstagram();
            return;
        }

        currentScreenIsReelViewer = false;
        gestureGate.hide();
        blockCover.hide();
        guardSystemScreen(packageName);
    }

    @Override
    public void onInterrupt() {
        currentScreenIsReelViewer = false;
        if (gestureGate != null) {
            gestureGate.hide();
        }
        if (blockCover != null) {
            blockCover.hide();
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
        long now = SystemClock.elapsedRealtime();
        if (now - lastHintElapsed >= HINT_COOLDOWN_MILLIS) {
            lastHintElapsed = now;
            Toast.makeText(this, "Save your brain from Doom", Toast.LENGTH_SHORT).show();
        }
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
                blockReelsTab(result, displayBounds);
                return;
            }
            if (result.screen == InstagramUiClassifier.Screen.REEL_VIEWER) {
                currentScreenIsReelViewer = true;
                blockCover.hide();
                recognitionStore.recordViewer(instagramVersion());
                Rect instagramBounds = new Rect();
                root.getBoundsInScreen(instagramBounds);
                gestureGate.show(instagramBounds);
                return;
            }
            currentScreenIsReelViewer = false;
            gestureGate.hide();
            blockCover.hide();
        } finally {
            result.recycle();
            root.recycle();
        }
    }

    private void blockReelsTab(InstagramUiClassifier.Result result, Rect displayBounds) {
        currentScreenIsReelViewer = false;
        gestureGate.hide();
        blockCover.show();
        recognitionStore.recordReelsTab(instagramVersion());

        long now = SystemClock.elapsedRealtime();
        if (now - lastTabActionElapsed < TAB_ACTION_COOLDOWN_MILLIS) {
            return;
        }
        lastTabActionElapsed = now;

        boolean clicked = result.homeNode != null
                && result.homeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!clicked) {
            Rect target = result.fallbackHomeBounds;
            float x = target == null ? displayBounds.width() * 0.10f : target.centerX();
            float y = target == null ? displayBounds.height() * 0.91f : target.centerY();
            dispatchTap(x, y);
        }
        Toast.makeText(this, "Reels tab is off", Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> {
            inspectCurrentWindow();
            blockCover.hide();
        }, 320L);
    }

    private void guardSystemScreen(String packageName) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastGuardElapsed < GUARD_COOLDOWN_MILLIS) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        try {
            if (!selfProtectionGuard.shouldClose(packageName, root)) {
                return;
            }
        } finally {
            if (root != null) {
                root.recycle();
            }
        }
        lastGuardElapsed = now;
        performGlobalAction(GLOBAL_ACTION_BACK);
        Toast.makeText(
                this,
                "No Doom is protecting you. Use the 60-minute request inside No Doom.",
                Toast.LENGTH_LONG).show();
        notifications.showGuardedScreenMessage();
    }

    private void dispatchTap(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0L, 1L))
                .build();
        dispatchGesture(gesture, null, handler);
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

