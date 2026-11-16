package com.chandsharma.nodoom;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

final class BlockCoverOverlay {
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FrameLayout view;
    private final WindowManager.LayoutParams layoutParams;
    private boolean attached;

    BlockCoverOverlay(AccessibilityService service) {
        windowManager = service.getSystemService(WindowManager.class);
        view = new FrameLayout(service);
        view.setBackgroundColor(Color.rgb(22, 18, 16));
        view.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        TextView message = new TextView(service);
        message.setText(com.chandsharma.nodoom.R.string.reels_tab_off);
        message.setTextColor(Color.WHITE);
        message.setTextSize(20);
        message.setTypeface(Typeface.DEFAULT_BOLD);
        message.setGravity(Gravity.CENTER);
        view.addView(message, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        layoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.setTitle("No Doom Reels tab cover");
    }

    void show() {
        handler.post(() -> {
            if (attached) {
                return;
            }
            try {
                windowManager.addView(view, layoutParams);
                attached = true;
            } catch (RuntimeException ignored) {
                attached = false;
            }
        });
    }

    void hide() {
        handler.post(() -> {
            if (!attached) {
                return;
            }
            try {
                windowManager.removeViewImmediate(view);
            } catch (RuntimeException ignored) {
                // The window may already be gone.
            }
            attached = false;
        });
    }
}

