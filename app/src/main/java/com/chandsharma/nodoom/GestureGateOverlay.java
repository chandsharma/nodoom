package com.chandsharma.nodoom;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Insets;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import java.util.ArrayList;
import java.util.List;

final class GestureGateOverlay {
    interface Callback {
        void onVerticalSwipeBlocked();

        boolean onTap(float x, float y);

        boolean shouldRestoreGate();
    }

    private final AccessibilityService service;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Callback callback;
    private final GateView gateView;
    private final WindowManager.LayoutParams layoutParams;
    private boolean attached;
    private boolean forwarding;
    private Rect targetBounds;

    GestureGateOverlay(AccessibilityService service, Callback callback) {
        this.service = service;
        this.callback = callback;
        this.windowManager = service.getSystemService(WindowManager.class);
        this.gateView = new GateView();
        this.layoutParams = new WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        this.layoutParams.gravity = Gravity.TOP | Gravity.START;
        this.layoutParams.setTitle("No Doom reel gesture gate");
    }

    void show(Rect bounds, int nativeNavigationTop) {
        if (bounds == null || bounds.isEmpty()) {
            return;
        }
        targetBounds = new Rect(bounds);
        if (nativeNavigationTop > targetBounds.top
                && nativeNavigationTop < targetBounds.bottom) {
            // Instagram owns this strip. Its bottom navigation remains genuinely native rather
            // than being intercepted and replayed by the gesture gate.
            targetBounds.bottom = nativeNavigationTop;
        }
        show();
    }

    private void show() {
        handler.post(() -> {
            if (forwarding || targetBounds == null || targetBounds.isEmpty()) {
                return;
            }
            if (!updateLayoutBounds()) {
                removeNow();
                return;
            }
            if (attached) {
                try {
                    windowManager.updateViewLayout(gateView, layoutParams);
                } catch (RuntimeException ignored) {
                    removeNow();
                }
                return;
            }
            try {
                windowManager.addView(gateView, layoutParams);
                attached = true;
            } catch (RuntimeException failure) {
                attached = false;
            }
        });
    }

    void hide() {
        targetBounds = null;
        handler.post(this::removeNow);
    }

    boolean isAttached() {
        return attached;
    }

    private void removeNow() {
        if (!attached) {
            return;
        }
        try {
            windowManager.removeViewImmediate(gateView);
        } catch (RuntimeException ignored) {
            // The system may already have removed the overlay during a window transition.
        }
        attached = false;
    }

    private void forwardGesture(List<PointF> points, long durationMillis) {
        removeNow();
        forwarding = true;

        Path path = new Path();
        PointF first = points.get(0);
        path.moveTo(first.x, first.y);
        for (int i = 1; i < points.size(); i++) {
            PointF point = points.get(i);
            path.lineTo(point.x, point.y);
        }

        long duration = Math.max(1L, Math.min(10_000L, durationMillis));
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0L, duration))
                .build();
        boolean accepted = service.dispatchGesture(
                gesture,
                new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        finishForwarding();
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        finishForwarding();
                    }
                },
                handler);
        if (!accepted) {
            finishForwarding();
        }
    }

    private void finishForwarding() {
        forwarding = false;
        handler.post(() -> {
            if (callback.shouldRestoreGate()) {
                show();
            }
        });
    }

    private final class GateView extends View {
        private final int touchSlop;
        private final List<PointF> points = new ArrayList<>();
        private float downX;
        private float downY;
        private long downTime;
        private boolean verticalSwipe;

        GateView() {
            super(service);
            setBackgroundColor(android.graphics.Color.TRANSPARENT);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            touchSlop = ViewConfiguration.get(service).getScaledTouchSlop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                points.clear();
                downX = event.getRawX();
                downY = event.getRawY();
                downTime = event.getEventTime();
                verticalSwipe = false;
                addPoint(event);
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN
                    || action == MotionEvent.ACTION_POINTER_UP) {
                addPoint(event);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                addPoint(event);
                float dx = event.getRawX() - downX;
                float dy = event.getRawY() - downY;
                if (SwipeClassifier.isVertical(dx, dy, touchSlop)) {
                    verticalSwipe = true;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                addPoint(event);
                long duration = Math.max(1L, event.getEventTime() - downTime);
                if (verticalSwipe) {
                    callback.onVerticalSwipeBlocked();
                } else if (action == MotionEvent.ACTION_UP && !points.isEmpty()) {
                    performClick();
                    if (!callback.onTap(event.getRawX(), event.getRawY())) {
                        forwardGesture(new ArrayList<>(points), duration);
                    }
                }
                points.clear();
                verticalSwipe = false;
                return true;
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        private void addPoint(MotionEvent event) {
            if (points.size() >= 48) {
                return;
            }
            points.add(new PointF(event.getRawX(), event.getRawY()));
        }
    }

    private boolean updateLayoutBounds() {
        if (targetBounds == null) {
            return false;
        }
        WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
        Rect displayBounds = metrics.getBounds();
        Insets safeInsets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars()
                        | WindowInsets.Type.displayCutout());

        // The status-bar strip deliberately stays outside the overlay. A swipe that begins there
        // belongs to Android's notification shade and must never be consumed by No Doom.
        int left = Math.max(targetBounds.left, displayBounds.left + safeInsets.left);
        int top = Math.max(targetBounds.top, displayBounds.top + safeInsets.top);
        int right = Math.min(targetBounds.right, displayBounds.right - safeInsets.right);
        int bottom = Math.min(targetBounds.bottom, displayBounds.bottom - safeInsets.bottom);
        if (right <= left || bottom <= top) {
            return false;
        }
        layoutParams.x = left;
        layoutParams.y = top;
        layoutParams.width = right - left;
        layoutParams.height = bottom - top;
        return true;
    }
}

