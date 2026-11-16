package com.chandsharma.nodoom;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String SETUP_PREFS = "setup_v1";
    private static final String KEY_WELCOME_ACCEPTED = "welcome_accepted";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";
    private static final int REQUEST_NOTIFICATIONS = 501;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTick = new Runnable() {
        @Override
        public void run() {
            render();
        }
    };

    private ProtectionStateStore stateStore;
    private AlarmScheduler alarmScheduler;
    private NotificationController notifications;
    private RecognitionStore recognitionStore;
    private SharedPreferences setupPreferences;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stateStore = new ProtectionStateStore(this);
        alarmScheduler = new AlarmScheduler(this);
        notifications = new NotificationController(this);
        recognitionStore = new RecognitionStore(this);
        setupPreferences = getSharedPreferences(SETUP_PREFS, MODE_PRIVATE);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color(com.chandsharma.nodoom.R.color.background));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(40));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refreshTick);
        ProtectionSnapshot snapshot = stateStore.get();
        alarmScheduler.sync(snapshot);
        notifications.refreshUserSettings();
        notifications.syncStateNotification(snapshot);
        render();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshTick);
        super.onPause();
    }

    private void render() {
        if (content == null) {
            return;
        }
        content.removeAllViews();
        addBrandHeader();

        if (!setupPreferences.getBoolean(KEY_WELCOME_ACCEPTED, false)) {
            renderWelcome();
            return;
        }

        ProtectionSnapshot snapshot = stateStore.get();

        renderProtectionCard(snapshot);
        renderSetupAndHealth();
        renderAbout();
        scheduleRefreshIfNeeded();
    }

    private void scheduleRefreshIfNeeded() {
        handler.removeCallbacks(refreshTick);
        ProtectionMode mode = stateStore.get().mode;
        if (mode == ProtectionMode.COUNTDOWN || mode == ProtectionMode.UNLOCK_WINDOW) {
            handler.postDelayed(refreshTick, 1_000L);
        }
    }

    private void addBrandHeader() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView eyebrow = text("NO DOOM", 12, color(com.chandsharma.nodoom.R.color.accent));
        eyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        eyebrow.setLetterSpacing(0.16f);
        top.addView(eyebrow, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button settings = new Button(this);
        settings.setText("Settings");
        settings.setTextSize(13);
        settings.setAllCaps(false);
        settings.setTextColor(color(com.chandsharma.nodoom.R.color.muted));
        settings.setBackgroundColor(Color.TRANSPARENT);
        settings.setMinHeight(0);
        settings.setMinWidth(0);
        settings.setPadding(dp(12), dp(8), 0, dp(8));
        settings.setOnClickListener(view -> startActivity(
                new Intent(this, SettingsActivity.class)));
        top.addView(settings);
        content.addView(top);

        TextView title = text("Instagram, one Reel at a time.", 30,
                color(com.chandsharma.nodoom.R.color.ink));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(8), 0, dp(6));
        content.addView(title);

        TextView subtitle = text(
                "Home, profiles and Explore stay normal. A full-screen Reel cannot advance to the next one.",
                16,
                color(com.chandsharma.nodoom.R.color.muted));
        subtitle.setLineSpacing(0, 1.12f);
        content.addView(subtitle);
        spacer(22);
    }

    private void renderWelcome() {
        LinearLayout card = card();
        card.addView(heading("A deliberate promise to yourself"));
        card.addView(body(
                "No Doom works beside the official Instagram app. It keeps every full-screen Reel viewer—including the Reels tab—on one Reel."));
        card.addView(body(
                "Turning protection off requires a 60-minute wait, followed by a 10-minute confirmation window. Protection stays active for the whole wait."));
        card.addView(body(
                "It has no Internet permission. Screen structure is checked on your phone and is never stored or sent anywhere."));
        Button start = primaryButton("Set up No Doom");
        start.setOnClickListener(view -> {
            setupPreferences.edit().putBoolean(KEY_WELCOME_ACCEPTED, true).apply();
            stateStore.turnOn();
            render();
        });
        card.addView(start);
        content.addView(card);
    }

    private void renderProtectionCard(ProtectionSnapshot snapshot) {
        LinearLayout card = card();
        int protectedColor = color(com.chandsharma.nodoom.R.color.protected_state);
        int accentColor = color(com.chandsharma.nodoom.R.color.accent);

        if (snapshot.mode == ProtectionMode.ON) {
            card.addView(kicker("PROTECTED", protectedColor));
            card.addView(heading("No Doom is ON"));
            card.addView(body("Full-screen next-Reel swipes are blocked quietly."));
            Button request = secondaryButton("Request to turn off");
            request.setOnClickListener(view -> confirmStartCountdown());
            card.addView(request);
        } else if (snapshot.mode == ProtectionMode.COUNTDOWN) {
            card.addView(kicker("PROTECTED · COOLING OFF", protectedColor));
            card.addView(heading(formatRemaining(snapshot.remainingMillis(
                    SystemClock.elapsedRealtime()))));
            card.addView(body("You can decide at " + formatClock(snapshot.displayDeadlineEpochMillis)
                    + ". No Doom remains fully active."));
            Button cancel = secondaryButton("Cancel request · stay protected");
            cancel.setOnClickListener(view -> {
                ProtectionSnapshot next = stateStore.cancelRequest();
                alarmScheduler.sync(next);
                notifications.syncStateNotification(next);
                render();
            });
            card.addView(cancel);
        } else if (snapshot.mode == ProtectionMode.UNLOCK_WINDOW) {
            card.addView(kicker("PROTECTED · DECISION WINDOW", accentColor));
            card.addView(heading(formatRemaining(snapshot.remainingMillis(
                    SystemClock.elapsedRealtime()))));
            card.addView(body(
                    "This window closes automatically. If you do nothing, No Doom stays ON."));
            Button turnOff = primaryButton("Turn off No Doom");
            turnOff.setOnClickListener(view -> confirmTurnOff());
            card.addView(turnOff);
            Button cancel = secondaryButton("Cancel · stay protected");
            cancel.setOnClickListener(view -> {
                ProtectionSnapshot next = stateStore.cancelRequest();
                alarmScheduler.sync(next);
                notifications.syncStateNotification(next);
                render();
            });
            card.addView(cancel);
        } else {
            card.addView(kicker("NOT PROTECTED", accentColor));
            card.addView(heading("No Doom is OFF"));
            card.addView(body("The one-Reel rule is paused until you turn protection back on."));
            Button turnOn = primaryButton("Turn on now");
            turnOn.setOnClickListener(view -> {
                ProtectionSnapshot next = stateStore.turnOn();
                alarmScheduler.sync(next);
                notifications.syncStateNotification(next);
                render();
            });
            card.addView(turnOn);
        }
        content.addView(card);
        spacer(14);
    }

    private void renderSetupAndHealth() {
        boolean accessibility = isAccessibilityEnabled();
        boolean notificationPermission = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        boolean exactAlarms = getSystemService(AlarmManager.class).canScheduleExactAlarms();
        boolean admin = getSystemService(DevicePolicyManager.class)
                .isAdminActive(adminComponent());
        boolean deviceReady = accessibility && notificationPermission && exactAlarms && admin;
        long lastViewer = recognitionStore.lastViewerMillis();
        long lastTab = recognitionStore.lastTabMillis();
        boolean liveCheck = lastViewer > 0L && lastTab > 0L;
        boolean complete = deviceReady && liveCheck;
        if (complete && !setupPreferences.getBoolean(KEY_SETUP_COMPLETE, false)) {
            setupPreferences.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply();
        }

        if (!deviceReady) {
            renderNextSetupStep(accessibility, notificationPermission, exactAlarms, admin);
            spacer(14);
        }

        LinearLayout accessCard = card();
        accessCard.addView(kicker("DEVICE ACCESS",
                deviceReady
                        ? color(com.chandsharma.nodoom.R.color.protected_state)
                        : color(com.chandsharma.nodoom.R.color.warning)));
        accessCard.addView(heading(deviceReady
                ? "All four safeguards are ready"
                : "Four clearly separated safeguards"));
        accessCard.addView(body(
                "These are Android permissions or safeguards. They are separate from the Instagram live test below."));
        addHealthRow(accessCard, "1 · Accessibility service", accessibility,
                accessibility ? "On" : "Recognises full-screen Reels");
        addHealthRow(accessCard, "2 · Notifications", notificationPermission,
                notificationPermission
                        ? "Allowed · routine protection alerts are off"
                        : "Only needed for the unlock-window alert");
        addHealthRow(accessCard, "3 · Exact timer", exactAlarms,
                exactAlarms ? "Allowed" : "Opens the decision window on time");
        addHealthRow(accessCard, "4 · Uninstall barrier", admin,
                admin ? "Active · removal has its own 10-minute path"
                        : "Prevents impulsive uninstalling");
        Button notificationSettings = secondaryButton("Notification and removal settings");
        notificationSettings.setOnClickListener(view -> startActivity(
                new Intent(this, SettingsActivity.class)));
        accessCard.addView(notificationSettings);
        content.addView(accessCard);
        spacer(14);

        String instagramVersion = instagramVersion();
        LinearLayout testCard = card();
        testCard.addView(kicker("INSTAGRAM LIVE CHECK",
                liveCheck
                        ? color(com.chandsharma.nodoom.R.color.protected_state)
                        : color(com.chandsharma.nodoom.R.color.warning)));
        testCard.addView(heading(liveCheck
                ? "Both behaviours were recognised"
                : "Test after device access is ready"));
        testCard.addView(body(
                "These rows are test evidence, not permissions. Their timestamps update only after No Doom sees the matching Instagram screen."));
        addHealthRow(testCard, "Official Instagram", !instagramVersion.equals("Not installed"),
                instagramVersion);
        addHealthRow(testCard, "Full-screen Reel recognised", lastViewer > 0L,
                lastViewer > 0L ? formatDateTime(lastViewer) : "Not seen yet");
        addHealthRow(testCard, "Reels tab recognised", lastTab > 0L,
                lastTab > 0L ? formatDateTime(lastTab) : "Not tested yet");

        if (deviceReady && !instagramVersion.equals("Not installed")) {
            testCard.addView(note(
                    "Open a full-screen Reel and swipe vertically five times: it must stay on the same Reel. Then open the Reels tab and repeat. Home, profile grids and Explore must still scroll normally."));
            Button openInstagram = primaryButton("Open Instagram for live check");
            openInstagram.setOnClickListener(view -> openInstagram());
            testCard.addView(openInstagram);
        }

        long interruption = recognitionStore.lastInterruptionMillis();
        if (interruption > 0L) {
            testCard.addView(note("Protection was interrupted on "
                    + formatDateTime(interruption) + "."));
        }

        String recognizedVersion = recognitionStore.lastRecognizedVersion();
        if (!recognizedVersion.isBlank()
                && !recognizedVersion.equals(instagramVersion)
                && !instagramVersion.equals("Not installed")) {
            testCard.addView(note(
                    "Instagram updated from " + recognizedVersion + " to " + instagramVersion
                            + ". Confirm the live check again."));
            if (new UserSettings(this).compatibilityAlertEnabled()
                    && recognitionStore.shouldWarnForVersion(instagramVersion)) {
                notifications.showInstagramCompatibilityWarning(instagramVersion);
                recognitionStore.markVersionWarned(instagramVersion);
            }
        }

        content.addView(testCard);
        spacer(14);
    }

    private void renderNextSetupStep(
            boolean accessibility,
            boolean notificationPermission,
            boolean exactAlarms,
            boolean admin) {
        LinearLayout card = card();
        card.addView(kicker("GUIDED SETUP", color(com.chandsharma.nodoom.R.color.warning)));

        if (!accessibility) {
            card.addView(heading("Step 1 of 4 · Accessibility"));
            card.addView(body(
                    "Android requires this to recognise the full-screen Reel viewer and block only its vertical next-Reel gesture."));
            Button button = primaryButton("Open Accessibility settings");
            button.setOnClickListener(view -> startActivity(
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
            card.addView(button);
            card.addView(note(
                    "If the switch is restricted: App info → ⋮ → Allow restricted settings, then return here."));
        } else if (!notificationPermission) {
            card.addView(heading("Step 2 of 4 · Timer alert"));
            card.addView(body(
                    "This permission is used for the one alert when your 60-minute wait ends. There is no permanent protection notification."));
            Button button = primaryButton("Allow notifications");
            button.setOnClickListener(view -> requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS));
            card.addView(button);
        } else if (!exactAlarms) {
            card.addView(heading("Step 3 of 4 · Exact timer"));
            card.addView(body(
                    "This lets the 10-minute decision window open on time without running a background service."));
            Button button = primaryButton("Allow alarms and reminders");
            button.setOnClickListener(view -> startActivity(new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()))));
            card.addView(button);
        } else if (!admin) {
            card.addView(heading("Step 4 of 4 · Uninstall barrier"));
            card.addView(body(
                    "This adds a deliberate 10-minute removal path. No Doom requests no lock, wipe or password policy."));
            Button button = primaryButton("Activate uninstall barrier");
            button.setOnClickListener(view -> {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent())
                        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "No Doom uses this only as an uninstall barrier. It cannot erase or lock your phone.");
                startActivity(intent);
            });
            card.addView(button);
        }
        content.addView(card);
    }

    private void renderAbout() {
        LinearLayout card = card();
        card.addView(kicker("ABOUT", color(com.chandsharma.nodoom.R.color.muted)));
        card.addView(heading("Private by construction"));
        card.addView(body(
                "No Doom 0.2.0-alpha.2 works with the official Play Store Instagram app. It has no Internet permission, no analytics, no account and no root requirement."));
        card.addView(note(
                "Accessibility can see Instagram and the small set of Android system screens needed for self-protection. Nothing it sees is saved or transmitted."));
        content.addView(card);
    }

    private void confirmStartCountdown() {
        new AlertDialog.Builder(this)
                .setTitle("Start the 60-minute wait?")
                .setMessage("No Doom stays fully active. After 60 minutes, you will have 10 minutes to confirm. If you miss that window, the request expires.")
                .setNegativeButton("Stay protected", null)
                .setPositiveButton("Start waiting", (dialog, which) -> {
                    ProtectionSnapshot next = stateStore.requestTurnOff();
                    alarmScheduler.sync(next);
                    notifications.syncStateNotification(next);
                    render();
                })
                .show();
    }

    private void confirmTurnOff() {
        new AlertDialog.Builder(this)
                .setTitle("Turn off No Doom?")
                .setMessage("Full-screen next-Reel swipes will no longer be blocked. Turning No Doom back on is instant.")
                .setNegativeButton("Keep it on", null)
                .setPositiveButton("Turn off", (dialog, which) -> {
                    ProtectionSnapshot next = stateStore.confirmTurnOff();
                    alarmScheduler.sync(next);
                    notifications.syncStateNotification(next);
                    render();
                })
                .show();
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        List<AccessibilityServiceInfo> enabled = manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : enabled) {
            if (info.getResolveInfo() != null
                    && info.getResolveInfo().serviceInfo != null
                    && getPackageName().equals(info.getResolveInfo().serviceInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private ComponentName adminComponent() {
        return new ComponentName(this, NoDoomDeviceAdminReceiver.class);
    }

    private String instagramVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.instagram.android", 0);
            return info.versionName == null ? String.valueOf(info.getLongVersionCode()) : info.versionName;
        } catch (PackageManager.NameNotFoundException notInstalled) {
            return "Not installed";
        }
    }

    private void openInstagram() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (intent == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Instagram not found")
                    .setMessage("Install the official Instagram app from Google Play, then return to the live check.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        startActivity(intent);
    }

    private void addHealthRow(LinearLayout parent, String label, boolean healthy, String detail) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        row.setPadding(0, dp(9), 0, dp(9));

        TextView mark = text(healthy ? "✓" : "!", 18,
                healthy
                        ? color(com.chandsharma.nodoom.R.color.protected_state)
                        : color(com.chandsharma.nodoom.R.color.warning));
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(mark, new LinearLayout.LayoutParams(dp(28),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView labelView = text(label, 15, color(com.chandsharma.nodoom.R.color.ink));
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        copy.addView(labelView);
        copy.addView(text(detail, 13, color(com.chandsharma.nodoom.R.color.muted)));
        row.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(color(com.chandsharma.nodoom.R.color.surface));
        background.setCornerRadius(dp(22));
        background.setStroke(dp(1), color(com.chandsharma.nodoom.R.color.divider));
        card.setBackground(background);
        card.setElevation(dp(1));
        return card;
    }

    private TextView heading(String value) {
        TextView view = text(value, 22, color(com.chandsharma.nodoom.R.color.ink));
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
        TextView view = text(value, 15, color(com.chandsharma.nodoom.R.color.muted));
        view.setLineSpacing(0, 1.12f);
        view.setPadding(0, 0, 0, dp(13));
        return view;
    }

    private TextView note(String value) {
        TextView view = text(value, 13, color(com.chandsharma.nodoom.R.color.muted));
        view.setLineSpacing(0, 1.1f);
        view.setPadding(0, dp(8), 0, dp(10));
        return view;
    }

    private TextView text(String value, int sp, int textColor) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(textColor);
        view.setTextIsSelectable(false);
        return view;
    }

    private Button primaryButton(String label) {
        Button button = button(label);
        button.setTextColor(Color.WHITE);
        GradientDrawable background = new GradientDrawable();
        background.setColor(color(com.chandsharma.nodoom.R.color.accent));
        background.setCornerRadius(dp(14));
        button.setBackground(background);
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = button(label);
        button.setTextColor(color(com.chandsharma.nodoom.R.color.ink));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.TRANSPARENT);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), color(com.chandsharma.nodoom.R.color.divider));
        button.setBackground(background);
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
        View view = new View(this);
        content.addView(view, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private String formatRemaining(long remainingMillis) {
        long totalSeconds = Math.max(0L, (remainingMillis + 999L) / 1_000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private String formatClock(long epochMillis) {
        return DateFormat.getTimeFormat(this).format(new Date(epochMillis));
    }

    private String formatDateTime(long epochMillis) {
        return DateFormat.getMediumDateFormat(this).format(new Date(epochMillis))
                + " · " + formatClock(epochMillis);
    }

    private int color(int resourceId) {
        return getColor(resourceId);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

