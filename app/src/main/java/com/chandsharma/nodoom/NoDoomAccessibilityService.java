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
    private static final long MODAL_LAUNCH_TIMEOUT_MILLIS = 1_000L;
    private static final long CONTENT_INSPECTION_DEBOUNCE_MILLIS = 80L;
    private static final long MODAL_WATCHDOG_MILLIS = 350L;
    private static final long MODAL_RETURN_GUARD_MILLIS = 1_000L;
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
    private boolean modalLaunchPending;
    private boolean modalSurfaceActive;
    private boolean protectedViewerBehindModal;
    private long modalLaunchDeadlineElapsed;
    private long modalReturnGuardUntilElapsed;
    private boolean modalWatchdogScheduled;
    private int modalWatchGeneration;
    private boolean protectedInspectionScheduled;
    private int protectedInspectionGeneration;
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
            // always receive their own gestures. Ignore background status-bar updates while
            // Instagram remains the active window.
            String activePackage = activeWindowPackage();
            if (WindowSafetyPolicy.systemUiObscures(packageName, activePackage)) {
                systemUiObscuring = true;
                suspendGestureGate();
            } else if (WindowSafetyPolicy.systemUiReleased(
                    packageName,
                    activePackage,
                    systemUiObscuring)) {
                systemUiObscuring = false;
                ProtectedAppModule activeModule = appRegistry.find(activePackage);
                if (activeModule != null) {
                    currentModule = activeModule;
                    handler.post(this::inspectCurrentWindow);
                }
            }
            return;
        }

        ProtectedAppModule module = appRegistry.find(packageName);
        if (module != null) {
            systemUiObscuring = false;
            currentModule = module;
            if (trackNavigationClick(module, event)) {
                return;
            }
            int eventType = event.getEventType();
            if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    || eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                // Instagram can emit hundreds of content updates while a Reel and a large share
                // sheet animate. Walking the remote accessibility tree for every update starves
                // overlay input and Android kills the service as unresponsive. Coalesce ordinary
                // changes. A protected viewer is already fail-closed, while a known modal has its
                // own watchdog; only an unclassified Instagram surface needs a delayed rescan.
                if (!modalSurfaceActive && !currentScreenIsProtectedViewer) {
                    scheduleProtectedInspection(CONTENT_INSPECTION_DEBOUNCE_MILLIS);
                }
                return;
            }
            inspectProtectedApp(module);
            return;
        }

        systemUiObscuring = false;
        if (!WindowSafetyPolicy.eventBelongsToActiveWindow(
                packageName,
                activeWindowPackage())) {
            // TYPE_ACCESSIBILITY_OVERLAY window updates are attributed to No Doom even while
            // Instagram remains active. They are not an app switch.
            return;
        }
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
    public GestureGateOverlay.VerticalGestureResult onModalVerticalGesture(float deltaY) {
        if (currentModule == null || stateStore == null
                || !stateStore.get().mode.isProtected()
                || systemUiObscuring
                || !modalSurfaceActive) {
            return GestureGateOverlay.VerticalGestureResult.BLOCK;
        }
        int action = deltaY < 0f
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
        // Never traverse Instagram's tree from the input callback. Share sheets can contain
        // hundreds of contacts, and keeping MotionEvent dispatch open while walking that tree can
        // trigger Android's five-second input ANR. Consume the gesture now and scroll the known
        // foreground list after input dispatch has returned.
        handler.post(() -> performModalScroll(action));
        return GestureGateOverlay.VerticalGestureResult.HANDLED;
    }

    private void performModalScroll(int action) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) {
            if (root != null) {
                root.recycle();
            }
            return;
        }
        try {
            ProtectedAppModule module = appRegistry.find(root.getPackageName().toString());
            if (module != null) {
                module.performModalScroll(root, action);
            }
        } finally {
            root.recycle();
        }
    }

    @Override
    public GestureGateOverlay.TapResult onTap(float x, float y) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return GestureGateOverlay.TapResult.NOT_HANDLED;
        }
        try {
            CharSequence packageName = root.getPackageName();
            ProtectedAppModule module = packageName == null
                    ? null
                    : appRegistry.find(packageName.toString());
            if (module == null) {
                return GestureGateOverlay.TapResult.NOT_HANDLED;
            }
            NodeTapResult result = performDeepestTap(
                    module,
                    root,
                    Math.round(x),
                    Math.round(y));
            if (result == NodeTapResult.MODAL_HANDLED) {
                return GestureGateOverlay.TapResult.OPENS_MODAL;
            }
            return GestureGateOverlay.TapResult.NOT_HANDLED;
        } finally {
            root.recycle();
        }
    }

    @Override
    public void onForwardedGestureFinished() {
        // A long press can show and dismiss a transient Instagram playback surface without a
        // final accessibility event. Reinspect independently so the protected viewer can never
        // remain fail-open after native tap/hold handling finishes.
        handler.postDelayed(this::inspectCurrentWindow, 25L);
        handler.postDelayed(this::inspectCurrentWindow, 100L);
        handler.postDelayed(this::inspectCurrentWindow, 250L);
    }

    private NodeTapResult performDeepestTap(
            ProtectedAppModule module,
            AccessibilityNodeInfo node,
            int x,
            int y) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (!bounds.contains(x, y)) {
            return NodeTapResult.NO_TARGET;
        }
        for (int index = node.getChildCount() - 1; index >= 0; index--) {
            AccessibilityNodeInfo child = node.getChild(index);
            if (child == null) {
                continue;
            }
            try {
                NodeTapResult result = performDeepestTap(module, child, x, y);
                if (result != NodeTapResult.NO_TARGET) {
                    return result;
                }
            } finally {
                child.recycle();
            }
        }
        if (!node.isClickable()) {
            return NodeTapResult.NO_TARGET;
        }
        if (!module.opensModal(node.getText(), node.getContentDescription())) {
            return NodeTapResult.NATIVE_GESTURE;
        }

        boolean wasBehindModal = protectedViewerBehindModal;
        protectedViewerBehindModal = protectedViewerBehindModal
                || currentScreenIsProtectedViewer
                || explicitInfiniteFeedSession;
        modalLaunchPending = true;
        modalLaunchDeadlineElapsed = SystemClock.elapsedRealtime()
                + MODAL_LAUNCH_TIMEOUT_MILLIS;
        if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            modalLaunchPending = false;
            modalLaunchDeadlineElapsed = 0L;
            protectedViewerBehindModal = wasBehindModal;
            return NodeTapResult.NATIVE_GESTURE;
        }
        scheduleModalInspections();
        return NodeTapResult.MODAL_HANDLED;
    }

    private void scheduleModalInspections() {
        handler.postDelayed(this::inspectCurrentWindow, 40L);
        handler.postDelayed(this::inspectCurrentWindow, 120L);
        handler.postDelayed(this::inspectCurrentWindow, 300L);
        handler.postDelayed(this::inspectCurrentWindow, 650L);
        handler.postDelayed(this::inspectCurrentWindow, MODAL_LAUNCH_TIMEOUT_MILLIS);
    }

    @Override
    public boolean shouldRestoreGate() {
        if (currentModule == null || stateStore == null || modalLaunchPending) {
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
            boolean protectedSurface = currentScreenIsProtectedViewer
                    || protectedViewerBehindModal
                    || modalLaunchPending;
            return WindowSafetyPolicy.canGate(
                    activePackage,
                    currentModule.packageName(),
                    systemUiObscuring,
                    protectedSurface,
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

    private String activeWindowPackage() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return "";
        }
        try {
            return root.getPackageName() == null ? "" : root.getPackageName().toString();
        } finally {
            root.recycle();
        }
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
            if (modalLaunchPending) {
                if (detection.surface == ProtectedAppModule.Surface.MODAL) {
                    modalLaunchPending = false;
                    modalLaunchDeadlineElapsed = 0L;
                } else if (now < modalLaunchDeadlineElapsed) {
                    return;
                } else {
                    modalLaunchPending = false;
                    modalLaunchDeadlineElapsed = 0L;
                }
            }

            if (detection.surface == ProtectedAppModule.Surface.MODAL) {
                modalSurfaceActive = true;
                protectedViewerBehindModal = protectedViewerBehindModal
                        || currentScreenIsProtectedViewer
                        || explicitInfiniteFeedSession
                        || detection.strongViewerSignature;
                clearViewerCandidate();
                currentScreenIsProtectedViewer = false;
                viewerExitGeneration++;
                // Cover the whole app surface while the sheet is open. Instagram's share action
                // row can resemble bottom navigation; clipping at that false boundary would leave
                // a small path for a closing-sheet swipe to reach the Reel underneath.
                showGestureGate(root, 0);
                scheduleModalWatchdog();
                return;
            }
            modalSurfaceActive = false;

            if (ReelControlPolicy.shouldProtectDuringModalReturn(
                    detection.otherNavigationSelected,
                    now,
                    modalReturnGuardUntilElapsed)) {
                enterProtectedViewer(module, root, detection.nativeNavigationTop);
                return;
            }
            if (modalReturnGuardUntilElapsed > 0L && now >= modalReturnGuardUntilElapsed) {
                modalReturnGuardUntilElapsed = 0L;
            }

            if (ReelControlPolicy.shouldRestoreAfterModal(
                    protectedViewerBehindModal,
                    detection.otherNavigationSelected)) {
                // Instagram can briefly omit its selected-Reels state while a full-screen sheet
                // closes, including the independent viewer signature itself. The modal was
                // launched from a protected viewer and no other navigation target is selected,
                // so fail closed before that transient ordinary state can clear the session.
                modalReturnGuardUntilElapsed = now + MODAL_RETURN_GUARD_MILLIS;
                enterProtectedViewer(module, root, detection.nativeNavigationTop);
                return;
            }

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
                enterProtectedViewer(module, root, detection.nativeNavigationTop);
                return;
            }
            if (detection.surface == ProtectedAppModule.Surface.SINGLE_ITEM_VIEWER) {
                if (currentScreenIsProtectedViewer
                        || explicitInfiniteFeedSession
                        || protectedViewerBehindModal) {
                    enterProtectedViewer(module, root, detection.nativeNavigationTop);
                } else {
                    confirmViewerCandidate(module, root, detection.nativeNavigationTop);
                }
                return;
            }
            // Some Instagram versions do not expose a selected state on the Reels navigation item.
            // A click-established Reels session plus the independent full-screen signature is safe.
            if (explicitInfiniteFeedSession
                    && detection.strongViewerSignature
                    && !detection.otherNavigationSelected) {
                recordInfiniteFeedIfNeeded(module);
                enterProtectedViewer(module, root, detection.nativeNavigationTop);
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
            AccessibilityNodeInfo root,
            int nativeNavigationTop) {
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
            enterProtectedViewer(module, root, nativeNavigationTop);
            return;
        }
        handler.postDelayed(
                this::inspectCurrentWindow,
                Math.max(1L, VIEWER_CONFIRMATION_MILLIS - age));
    }

    private void enterProtectedViewer(
            ProtectedAppModule module,
            AccessibilityNodeInfo root,
            int nativeNavigationTop) {
        clearViewerCandidate();
        viewerExitGeneration++;
        currentModule = module;
        currentScreenIsProtectedViewer = true;
        modalSurfaceActive = false;
        modalLaunchPending = false;
        modalLaunchDeadlineElapsed = 0L;
        protectedViewerBehindModal = false;
        cancelModalWatchdog();
        recordViewerIfNeeded(module);
        showGestureGate(root, nativeNavigationTop);
    }

    private void showGestureGate(AccessibilityNodeInfo root, int nativeNavigationTop) {
        Rect appBounds = new Rect();
        root.getBoundsInScreen(appBounds);
        gestureGate.show(appBounds, nativeNavigationTop);
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
        modalSurfaceActive = false;
        modalLaunchPending = false;
        modalLaunchDeadlineElapsed = 0L;
        protectedViewerBehindModal = false;
        modalReturnGuardUntilElapsed = 0L;
        cancelModalWatchdog();
        if (clearInfiniteFeedSession) {
            clearInfiniteFeedSession();
        }
        gestureGate.hide();
    }

    private void suspendGestureGate() {
        viewerExitGeneration++;
        cancelProtectedInspection();
        clearViewerCandidate();
        currentScreenIsProtectedViewer = false;
        modalSurfaceActive = false;
        if (gestureGate != null) {
            gestureGate.hide();
        }
    }

    private void resetProtectionSession(boolean clearInfiniteFeedSession) {
        suspendGestureGate();
        currentModule = null;
        modalLaunchPending = false;
        modalLaunchDeadlineElapsed = 0L;
        protectedViewerBehindModal = false;
        modalReturnGuardUntilElapsed = 0L;
        cancelModalWatchdog();
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

    private void scheduleModalWatchdog() {
        if (modalWatchdogScheduled) {
            return;
        }
        modalWatchdogScheduled = true;
        int generation = modalWatchGeneration;
        handler.postDelayed(() -> {
            if (generation != modalWatchGeneration) {
                return;
            }
            modalWatchdogScheduled = false;
            if (protectedViewerBehindModal && !currentScreenIsProtectedViewer) {
                inspectCurrentWindow();
            }
        }, MODAL_WATCHDOG_MILLIS);
    }

    private void scheduleProtectedInspection(long delayMillis) {
        if (protectedInspectionScheduled) {
            return;
        }
        protectedInspectionScheduled = true;
        int generation = protectedInspectionGeneration;
        handler.postDelayed(() -> {
            if (generation != protectedInspectionGeneration) {
                return;
            }
            protectedInspectionScheduled = false;
            inspectCurrentWindow();
        }, delayMillis);
    }

    private void cancelProtectedInspection() {
        protectedInspectionGeneration++;
        protectedInspectionScheduled = false;
    }

    private void cancelModalWatchdog() {
        modalWatchGeneration++;
        modalWatchdogScheduled = false;
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
        // The service receives foreground-package transitions globally so it can remove a stale
        // overlay immediately. Never request or traverse another app's tree here.
        if (!selfProtectionGuard.handlesPackage(packageName)) {
            return;
        }
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

    private enum NodeTapResult {
        NO_TARGET,
        NATIVE_GESTURE,
        MODAL_HANDLED
    }
}
