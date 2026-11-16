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
import android.widget.Toast;

public final class NoDoomAccessibilityService extends AccessibilityService
        implements GestureGateOverlay.Callback {
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final long VIEWER_CONFIRMATION_MILLIS = 80L;
    private static final long INFINITE_FEED_TRANSITION_GRACE_MILLIS = 1_000L;
    private static final long UNCERTAIN_EXIT_DEBOUNCE_MILLIS = 100L;
    private static final long GUARD_COOLDOWN_MILLIS = 900L;
    private static final long RECOGNITION_WRITE_COOLDOWN_MILLIS = 5_000L;
    private static final long BLOCK_HINT_COOLDOWN_MILLIS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ProtectedAppRegistry appRegistry = new ProtectedAppRegistry();
    private final SelfProtectionGuard selfProtectionGuard = new SelfProtectionGuard();

    private ProtectionStateStore stateStore;
    private RemovalStateStore removalStateStore;
    private RecognitionStore recognitionStore;
    private UserSettings userSettings;
    private GestureGateOverlay gestureGate;
    private ProtectedAppModule currentModule;
    private boolean currentScreenIsProtectedViewer;
    private boolean explicitInfiniteFeedSession;
    private long explicitInfiniteFeedStartedElapsed;
    private boolean systemUiObscuring;
    private String viewerCandidatePackage;
    private long viewerCandidateSinceElapsed;
    private long lastViewerRecordElapsed;
    private long lastTabRecordElapsed;
    private long lastGuardElapsed;
    private long lastBlockHintElapsed;
    private int viewerExitGeneration;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        stateStore = new ProtectionStateStore(this);
        removalStateStore = new RemovalStateStore(this);
        recognitionStore = new RecognitionStore(this);
        userSettings = new UserSettings(this);
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
            resetProtectionSession(true);
            return;
        }

        CharSequence packageValue = event.getPackageName();
        String packageName = packageValue == null ? "" : packageValue.toString();
        if (SYSTEM_UI_PACKAGE.equals(packageName)) {
            // Notification shade, Quick Settings, volume UI and other trusted system surfaces must
            // always receive their own gestures. Keep only the explicit Reels-session memory so it
            // can be restored after the shade closes.
            systemUiObscuring = true;
            suspendGestureGate();
            return;
        }

        ProtectedAppModule module = appRegistry.find(packageName);
        if (module != null) {
            systemUiObscuring = false;
            currentModule = module;
            if (trackNavigationClick(module, event)) {
                return;
            }
            inspectProtectedApp(module);
            return;
        }

        systemUiObscuring = false;
        resetProtectionSession(true);
        guardSystemScreen(packageName);
    }

    @Override
    public void onInterrupt() {
        resetProtectionSession(true);
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
        if (userSettings == null || !userSettings.blockedSwipeHintEnabled()) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastBlockHintElapsed < BLOCK_HINT_COOLDOWN_MILLIS) {
            return;
        }
        lastBlockHintElapsed = now;
        Toast.makeText(this, "One Reel only", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean shouldRestoreGate() {
        if (currentModule == null || stateStore == null) {
            return false;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return false;
        }
        try {
            String activePackage = root.getPackageName() == null
                    ? ""
                    : root.getPackageName().toString();
            return WindowSafetyPolicy.canGate(
                    activePackage,
                    currentModule.packageName(),
                    systemUiObscuring,
                    currentScreenIsProtectedViewer,
                    stateStore.get().mode.isProtected());
        } finally {
            root.recycle();
        }
    }

    private void inspectCurrentWindow() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) {
            suspendGestureGate();
            if (root != null) {
                root.recycle();
            }
            return;
        }
        String packageName = root.getPackageName().toString();
        root.recycle();

        if (SYSTEM_UI_PACKAGE.equals(packageName)) {
            systemUiObscuring = true;
            suspendGestureGate();
            return;
        }
        ProtectedAppModule module = appRegistry.find(packageName);
        if (module != null) {
            systemUiObscuring = false;
            currentModule = module;
            inspectProtectedApp(module);
            return;
        }
        resetProtectionSession(true);
        guardSystemScreen(packageName);
    }

    private void inspectProtectedApp(ProtectedAppModule module) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !module.packageName().contentEquals(root.getPackageName())) {
            if (root != null) {
                root.recycle();
            }
            suspendGestureGate();
            return;
        }

        Rect displayBounds = getSystemService(WindowManager.class)
                .getCurrentWindowMetrics()
                .getBounds();
        ProtectedAppModule.Detection detection = module.inspect(
                root,
                displayBounds.width(),
                displayBounds.height());
        try {
            long now = SystemClock.elapsedRealtime();
            if (InfiniteFeedSessionPolicy.shouldClear(
                    explicitInfiniteFeedSession,
                    now - explicitInfiniteFeedStartedElapsed,
                    INFINITE_FEED_TRANSITION_GRACE_MILLIS,
                    detection.otherNavigationSelected,
                    detection.surface == ProtectedAppModule.Surface.ORDINARY,
                    detection.strongViewerSignature)) {
                clearInfiniteFeedSession();
            }

            if (detection.surface == ProtectedAppModule.Surface.INFINITE_FEED) {
                startInfiniteFeedSession(false);
                recordInfiniteFeedIfNeeded(module);
                enterProtectedViewer(module, root);
                return;
            }
            if (detection.surface == ProtectedAppModule.Surface.SINGLE_ITEM_VIEWER) {
                if (currentScreenIsProtectedViewer || explicitInfiniteFeedSession) {
                    enterProtectedViewer(module, root);
                } else {
                    confirmViewerCandidate(module, root);
                }
                return;
            }
            if (detection.surface == ProtectedAppModule.Surface.MODAL) {
                clearViewerCandidate();
                currentScreenIsProtectedViewer = false;
                viewerExitGeneration++;
                gestureGate.hide();
                return;
            }

            // Some Instagram versions do not expose a selected state on the Reels navigation item.
            // A click-established Reels session plus the independent full-screen signature is safe.
            if (explicitInfiniteFeedSession
                    && detection.strongViewerSignature
                    && !detection.otherNavigationSelected) {
                recordInfiniteFeedIfNeeded(module);
                enterProtectedViewer(module, root);
                return;
            }

            if (detection.surface == ProtectedAppModule.Surface.ORDINARY) {
                exitViewerImmediately(false);
                return;
            }
            scheduleUncertainViewerExit();
        } finally {
            root.recycle();
        }
    }

    private void confirmViewerCandidate(
            ProtectedAppModule module,
            AccessibilityNodeInfo root) {
        long now = SystemClock.elapsedRealtime();
        if (!module.packageName().equals(viewerCandidatePackage)) {
            viewerCandidatePackage = module.packageName();
            viewerCandidateSinceElapsed = now;
            gestureGate.hide();
            handler.postDelayed(this::inspectCurrentWindow, VIEWER_CONFIRMATION_MILLIS);
            return;
        }
        long age = now - viewerCandidateSinceElapsed;
        if (age >= VIEWER_CONFIRMATION_MILLIS) {
            enterProtectedViewer(module, root);
            return;
        }
        handler.postDelayed(
                this::inspectCurrentWindow,
                Math.max(1L, VIEWER_CONFIRMATION_MILLIS - age));
    }

    private void enterProtectedViewer(
            ProtectedAppModule module,
            AccessibilityNodeInfo root) {
        clearViewerCandidate();
        viewerExitGeneration++;
        currentModule = module;
        currentScreenIsProtectedViewer = true;
        recordViewerIfNeeded(module);
        Rect appBounds = new Rect();
        root.getBoundsInScreen(appBounds);
        gestureGate.show(appBounds);
    }

    private void scheduleUncertainViewerExit() {
        clearViewerCandidate();
        if (!currentScreenIsProtectedViewer) {
            gestureGate.hide();
            return;
        }
        int generation = ++viewerExitGeneration;
        handler.postDelayed(() -> {
            if (generation != viewerExitGeneration) {
                return;
            }
            currentScreenIsProtectedViewer = false;
            gestureGate.hide();
        }, UNCERTAIN_EXIT_DEBOUNCE_MILLIS);
    }

    private void exitViewerImmediately(boolean clearInfiniteFeedSession) {
        viewerExitGeneration++;
        clearViewerCandidate();
        currentScreenIsProtectedViewer = false;
        if (clearInfiniteFeedSession) {
            clearInfiniteFeedSession();
        }
        gestureGate.hide();
    }

    private void suspendGestureGate() {
        viewerExitGeneration++;
        clearViewerCandidate();
        currentScreenIsProtectedViewer = false;
        if (gestureGate != null) {
            gestureGate.hide();
        }
    }

    private void resetProtectionSession(boolean clearInfiniteFeedSession) {
        suspendGestureGate();
        currentModule = null;
        if (clearInfiniteFeedSession) {
            clearInfiniteFeedSession();
        }
    }

    private void startInfiniteFeedSession(boolean restartClock) {
        if (!explicitInfiniteFeedSession || restartClock) {
            explicitInfiniteFeedStartedElapsed = SystemClock.elapsedRealtime();
        }
        explicitInfiniteFeedSession = true;
    }

    private void clearInfiniteFeedSession() {
        explicitInfiniteFeedSession = false;
        explicitInfiniteFeedStartedElapsed = 0L;
    }

    private void clearViewerCandidate() {
        viewerCandidatePackage = null;
        viewerCandidateSinceElapsed = 0L;
    }

    /** @return true when the click starts a transition and the old window must not be inspected. */
    private boolean trackNavigationClick(
            ProtectedAppModule module,
            AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return false;
        }
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) {
            return false;
        }
        try {
            Rect displayBounds = getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics()
                    .getBounds();
            ProtectedAppModule.NavigationTarget target = module.navigationTarget(
                    source,
                    displayBounds.height());
            if (target == ProtectedAppModule.NavigationTarget.OTHER) {
                clearInfiniteFeedSession();
                exitViewerImmediately(true);
                return false;
            }
            if (target != ProtectedAppModule.NavigationTarget.INFINITE_FEED) {
                return false;
            }
            startInfiniteFeedSession(true);
            exitViewerImmediately(false);
            handler.postDelayed(this::inspectCurrentWindow, 25L);
            handler.postDelayed(this::inspectCurrentWindow, 100L);
            handler.postDelayed(this::inspectCurrentWindow, 250L);
            handler.postDelayed(this::inspectCurrentWindow, 600L);
            return true;
        } finally {
            source.recycle();
        }
    }

    private void recordViewerIfNeeded(ProtectedAppModule module) {
        if (!InstagramProtectionModule.PACKAGE_NAME.equals(module.packageName())) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastViewerRecordElapsed < RECOGNITION_WRITE_COOLDOWN_MILLIS) {
            return;
        }
        lastViewerRecordElapsed = now;
        recognitionStore.recordViewer(installedVersion(module.packageName()));
    }

    private void recordInfiniteFeedIfNeeded(ProtectedAppModule module) {
        if (!InstagramProtectionModule.PACKAGE_NAME.equals(module.packageName())) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastTabRecordElapsed < RECOGNITION_WRITE_COOLDOWN_MILLIS) {
            return;
        }
        lastTabRecordElapsed = now;
        recognitionStore.recordReelsTab(installedVersion(module.packageName()));
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

    private String installedVersion(String packageName) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
            return info.versionName == null
                    ? String.valueOf(info.getLongVersionCode())
                    : info.versionName;
        } catch (PackageManager.NameNotFoundException notInstalled) {
            return "unknown";
        }
    }
}
