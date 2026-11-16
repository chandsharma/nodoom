package com.chandsharma.nodoom;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

final class ProtectionStateStore {
    static final long COUNTDOWN_MILLIS = 60L * 60L * 1000L;
    static final long WINDOW_MILLIS = 10L * 60L * 1000L;

    private static final String PREFS = "protection_state_v1";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_MODE = "mode";
    private static final String KEY_DEADLINE_ELAPSED = "deadline_elapsed";
    private static final String KEY_DEADLINE_WALL = "deadline_wall";
    private static final String KEY_BOOT_COUNT = "boot_count";

    private final Context context;
    private final SharedPreferences preferences;

    ProtectionStateStore(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized ProtectionSnapshot get() {
        if (!preferences.getBoolean(KEY_INITIALIZED, false)) {
            return write(ProtectionMode.ON, 0L, 0L);
        }

        ProtectionMode mode;
        try {
            mode = ProtectionMode.valueOf(preferences.getString(KEY_MODE, ""));
        } catch (IllegalArgumentException | NullPointerException invalidState) {
            return write(ProtectionMode.ON, 0L, 0L);
        }

        int savedBootCount = preferences.getInt(KEY_BOOT_COUNT, Integer.MIN_VALUE);
        int currentBootCount = currentBootCount();
        if (savedBootCount != currentBootCount) {
            // OFF survives a reboot. Every protected transitional state returns to ON.
            return mode == ProtectionMode.OFF
                    ? write(ProtectionMode.OFF, 0L, 0L)
                    : write(ProtectionMode.ON, 0L, 0L);
        }

        long deadlineElapsed = preferences.getLong(KEY_DEADLINE_ELAPSED, 0L);
        long deadlineWall = preferences.getLong(KEY_DEADLINE_WALL, 0L);
        if ((mode == ProtectionMode.COUNTDOWN || mode == ProtectionMode.UNLOCK_WINDOW)
                && (deadlineElapsed <= 0L || deadlineWall <= 0L)) {
            return write(ProtectionMode.ON, 0L, 0L);
        }

        long now = SystemClock.elapsedRealtime();
        if (mode == ProtectionMode.COUNTDOWN && now >= deadlineElapsed) {
            long lateBy = now - deadlineElapsed;
            long remainingWindow = WINDOW_MILLIS - lateBy;
            if (remainingWindow <= 0L) {
                return write(ProtectionMode.ON, 0L, 0L);
            }
            return write(
                    ProtectionMode.UNLOCK_WINDOW,
                    now + remainingWindow,
                    System.currentTimeMillis() + remainingWindow);
        }
        if (mode == ProtectionMode.UNLOCK_WINDOW && now >= deadlineElapsed) {
            return write(ProtectionMode.ON, 0L, 0L);
        }
        return new ProtectionSnapshot(mode, deadlineElapsed, deadlineWall);
    }

    synchronized ProtectionSnapshot requestTurnOff() {
        ProtectionSnapshot current = get();
        if (current.mode != ProtectionMode.ON) {
            return current;
        }
        long nowElapsed = SystemClock.elapsedRealtime();
        long nowWall = System.currentTimeMillis();
        return write(
                ProtectionMode.COUNTDOWN,
                nowElapsed + COUNTDOWN_MILLIS,
                nowWall + COUNTDOWN_MILLIS);
    }

    synchronized ProtectionSnapshot cancelRequest() {
        ProtectionSnapshot current = get();
        if (current.mode == ProtectionMode.COUNTDOWN
                || current.mode == ProtectionMode.UNLOCK_WINDOW) {
            return write(ProtectionMode.ON, 0L, 0L);
        }
        return current;
    }

    synchronized ProtectionSnapshot confirmTurnOff() {
        ProtectionSnapshot current = get();
        if (current.mode != ProtectionMode.UNLOCK_WINDOW) {
            return current;
        }
        return write(ProtectionMode.OFF, 0L, 0L);
    }

    synchronized ProtectionSnapshot turnOn() {
        return write(ProtectionMode.ON, 0L, 0L);
    }

    synchronized ProtectionSnapshot resetProtectedStateAfterBoot() {
        ProtectionSnapshot current = get();
        if (current.mode == ProtectionMode.OFF) {
            return current;
        }
        return write(ProtectionMode.ON, 0L, 0L);
    }

    private ProtectionSnapshot write(
            ProtectionMode mode,
            long deadlineElapsed,
            long deadlineWall) {
        boolean committed = preferences.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putString(KEY_MODE, mode.name())
                .putLong(KEY_DEADLINE_ELAPSED, deadlineElapsed)
                .putLong(KEY_DEADLINE_WALL, deadlineWall)
                .putInt(KEY_BOOT_COUNT, currentBootCount())
                .commit();
        if (!committed) {
            // The fail-safe state is always protected, even if storage is unhealthy.
            return new ProtectionSnapshot(ProtectionMode.ON, 0L, 0L);
        }
        return new ProtectionSnapshot(mode, deadlineElapsed, deadlineWall);
    }

    private int currentBootCount() {
        return Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.BOOT_COUNT,
                -1);
    }
}

