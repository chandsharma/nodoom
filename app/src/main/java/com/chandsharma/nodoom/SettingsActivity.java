package com.chandsharma.nodoom;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

public final class SettingsActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 601;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTick = new Runnable() {
        @Override
        public void run() {
            render();
        }
    };

    private UserSettings userSettings;
    private RemovalStateStore removalStore;
    private NotificationController notifications;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userSettings = new UserSettings(this);
        removalStore = new RemovalStateStore(this);
        notifications = new NotificationController(this);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(getColor(R.color.background));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(40));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refreshTick);
        render();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshTick);
        super.onPause();
    }

    private void render() {
        content.removeAllViews();

        Button back = textButton("←  No Doom");
        back.setOnClickListener(view -> finish());
        content.addView(back);
        content.addView(kicker("SETTINGS", getColor(R.color.accent)));
        content.addView(title("Quiet by default"));
        content.addView(body(
                "Choose the alerts that matter. No Doom never shows routine protection popups inside Instagram."));
        spacer(18);

        renderNotificationSettings();
        spacer(14);
        renderInstagramFeedback();
        spacer(14);
        renderBehaviourSummary();
        spacer(14);
        renderRemovalSettings();
        scheduleRefreshIfNeeded();
    }

    private void scheduleRefreshIfNeeded() {
        handler.removeCallbacks(refreshTick);
        if (removalStore.get().mode != RemovalStateStore.Mode.IDLE) {
            handler.postDelayed(refreshTick, 1_000L);
        }
    }

    private void renderNotificationSettings() {
        LinearLayout card = card();
        card.addView(kicker("NOTIFICATIONS", getColor(R.color.protected_state)));
        card.addView(heading("All alerts are optional"));
        card.addView(body(
                "Routine protection stays quiet by default. Turn on only the Android notifications you want."));

        boolean permission = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        if (!permission) {
            card.addView(statusLine("Notification permission", false,
                    "Off — timer alerts cannot appear"));
            Button allow = primaryButton("Allow notification permission");
            allow.setOnClickListener(view -> requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS));
            card.addView(allow);
        }

        card.addView(toggle(
                "Always-on protection status",
                "Optional silent notification while No Doom is on.",
                userSettings.protectionStatusEnabled(),
                enabled -> {
                    userSettings.setProtectionStatusEnabled(enabled);
                    notifications.syncStateNotification(new ProtectionStateStore(this).get());
                }));
        card.addView(toggle(
                "Unlock-window alert",
                "Alert once after the 60-minute wait ends.",
                userSettings.unlockAlertEnabled(),
                enabled -> {
                    userSettings.setUnlockAlertEnabled(enabled);
                    notifications.refreshUserSettings();
                }));
        card.addView(toggle(
                "Countdown status",
                "Optional silent notification while a turn-off countdown is running.",
                userSettings.countdownStatusEnabled(),
                enabled -> {
                    userSettings.setCountdownStatusEnabled(enabled);
                    notifications.syncStateNotification(new ProtectionStateStore(this).get());
                }));
        card.addView(toggle(
                "Instagram-change warning",
                "Optional warning when Instagram updates and needs another live check.",
                userSettings.compatibilityAlertEnabled(),
                enabled -> {
                    userSettings.setCompatibilityAlertEnabled(enabled);
                    notifications.refreshUserSettings();
                }));

        Button system = secondaryButton("Open Android notification settings");
        system.setOnClickListener(view -> startActivity(new Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        card.addView(system);
        content.addView(card);
    }

    private void renderInstagramFeedback() {
        LinearLayout card = card();
        card.addView(kicker("INSTAGRAM FEEDBACK", getColor(R.color.protected_state)));
        card.addView(heading("Silent by default"));
        card.addView(body(
                "A blocked next-Reel swipe normally leaves the current Reel in place without a popup."));
        card.addView(toggle(
                "Blocked-swipe hint",
                "Optional short toast, limited to at most once every 30 seconds.",
                userSettings.blockedSwipeHintEnabled(),
                userSettings::setBlockedSwipeHintEnabled));
        content.addView(card);
    }

    private void renderBehaviourSummary() {
        LinearLayout card = card();
        card.addView(kicker("INSTAGRAM", getColor(R.color.protected_state)));
        card.addView(heading("One full-screen Reel"));
        card.addView(statusLine("Home, Explore and profiles", true,
                "Their grids and lists scroll normally"));
        card.addView(statusLine("Full-screen Reel viewer", true,
                "Vertical next-Reel swipes are blocked"));
        card.addView(statusLine("Reels tab", true,
                "The first Reel can play; the next one cannot"));
        card.addView(statusLine("Notification shade and Android UI", true,
                "Always receive their own gestures"));
        card.addView(note(
                "No toast, banner or notification appears when a swipe is blocked unless you enable feedback above."));
        content.addView(card);
    }

    private void renderRemovalSettings() {
        LinearLayout card = card();
        card.addView(kicker("REMOVE APP", getColor(R.color.warning)));
        card.addView(heading("A clear 10-minute path"));

        DevicePolicyManager policyManager = getSystemService(DevicePolicyManager.class);
        if (!policyManager.isAdminActive(adminComponent())) {
            card.addView(body(
                    "The uninstall barrier is already off. Android can uninstall No Doom normally."));
            Button uninstall = secondaryButton("Open uninstall screen");
            uninstall.setOnClickListener(view -> openUninstallScreen());
            card.addView(uninstall);
            content.addView(card);
            return;
        }

        RemovalStateStore.Snapshot removal = removalStore.get();
        if (removal.mode == RemovalStateStore.Mode.IDLE) {
            card.addView(body(
                    "Start a 10-minute wait. Protection stays active, then a 10-minute removal window opens."));
            Button start = secondaryButton("Start 10-minute removal timer");
            start.setOnClickListener(view -> {
                removalStore.requestRemoval();
                render();
            });
            card.addView(start);
        } else if (removal.mode == RemovalStateStore.Mode.WAITING) {
            card.addView(heading(formatRemaining(removal.remainingMillis())));
            card.addView(body("The uninstall barrier remains active until this reaches zero."));
            Button cancel = secondaryButton("Cancel removal request");
            cancel.setOnClickListener(view -> {
                removalStore.cancel();
                render();
            });
            card.addView(cancel);
        } else {
            card.addView(heading("Removal window · " + formatRemaining(removal.remainingMillis())));
            card.addView(body(
                    "No Doom will deactivate only its uninstall barrier, then Android will ask you to confirm uninstalling."));
            Button remove = primaryButton("Deactivate barrier and uninstall");
            remove.setOnClickListener(view -> confirmRemoval());
            card.addView(remove);
            Button cancel = secondaryButton("Cancel and keep No Doom");
            cancel.setOnClickListener(view -> {
                removalStore.cancel();
                render();
            });
            card.addView(cancel);
        }
        content.addView(card);
    }

    private void confirmRemoval() {
        if (!removalStore.removalAllowed()) {
            render();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Remove No Doom?")
                .setMessage("The uninstall barrier will be deactivated. Android will show its normal uninstall confirmation next.")
                .setNegativeButton("Keep No Doom", null)
                .setPositiveButton("Continue", (dialog, which) -> {
                    getSystemService(DevicePolicyManager.class).removeActiveAdmin(adminComponent());
                    handler.postDelayed(this::openUninstallScreen, 500L);
                })
                .show();
    }

    private void openUninstallScreen() {
        startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getPackageName())));
    }

    private ComponentName adminComponent() {
        return new ComponentName(this, NoDoomDeviceAdminReceiver.class);
    }

    private LinearLayout toggle(
            String label,
            String detail,
            boolean checked,
            ToggleListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView labelView = text(label, 15, getColor(R.color.ink));
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        copy.addView(labelView);
        copy.addView(text(detail, 13, getColor(R.color.muted)));
        row.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch control = new Switch(this);
        control.setChecked(checked);
        control.setContentDescription(label);
        control.setOnCheckedChangeListener((button, enabled) -> listener.onToggle(enabled));
        row.addView(control);
        return row;
    }

    private LinearLayout statusLine(String label, boolean good, String detail) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView mark = text(good ? "✓" : "!", 18,
                getColor(good ? R.color.protected_state : R.color.warning));
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(mark, new LinearLayout.LayoutParams(dp(28),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView labelView = text(label, 15, getColor(R.color.ink));
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        copy.addView(labelView);
        copy.addView(text(detail, 13, getColor(R.color.muted)));
        row.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.surface));
        background.setCornerRadius(dp(22));
        background.setStroke(dp(1), getColor(R.color.divider));
        card.setBackground(background);
        card.setElevation(dp(1));
        return card;
    }

    private TextView title(String value) {
        TextView view = text(value, 30, getColor(R.color.ink));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, dp(8), 0, dp(6));
        return view;
    }

    private TextView heading(String value) {
        TextView view = text(value, 22, getColor(R.color.ink));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, dp(5), 0, dp(7));
        return view;
    }

    private TextView kicker(String value, int textColor) {
        TextView view = text(value, 11, textColor);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setLetterSpacing(0.12f);
        return view;
    }

    private TextView body(String value) {
        TextView view = text(value, 15, getColor(R.color.muted));
        view.setLineSpacing(0, 1.12f);
        view.setPadding(0, 0, 0, dp(13));
        return view;
    }

    private TextView note(String value) {
        TextView view = text(value, 13, getColor(R.color.muted));
        view.setLineSpacing(0, 1.1f);
        view.setPadding(0, dp(8), 0, dp(8));
        return view;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private Button primaryButton(String label) {
        Button button = button(label);
        button.setTextColor(Color.WHITE);
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.accent));
        background.setCornerRadius(dp(14));
        button.setBackground(background);
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = button(label);
        button.setTextColor(getColor(R.color.ink));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.TRANSPARENT);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), getColor(R.color.divider));
        button.setBackground(background);
        return button;
    }

    private Button textButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setTextColor(getColor(R.color.muted));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(50));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        button.setLayoutParams(params);
        return button;
    }

    private void spacer(int heightDp) {
        content.addView(new View(this), new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private String formatRemaining(long remainingMillis) {
        long totalSeconds = Math.max(0L, (remainingMillis + 999L) / 1_000L);
        return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60L,
                totalSeconds % 60L);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface ToggleListener {
        void onToggle(boolean enabled);
    }
}
