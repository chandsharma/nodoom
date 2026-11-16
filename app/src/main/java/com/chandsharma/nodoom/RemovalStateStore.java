package com.chandsharma.nodoom;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

final class RemovalStateStore {
    static final long WAIT_MILLIS = 10L * 60L * 1000L;
    static final long READY_WINDOW_MILLIS = 10L * 60L * 1000L;

    enum Mode {
        IDLE,
        WAITING,
        READY
    }

    static final class Snapshot {
        final Mode mode;
        final long deadlineElapsedRealtime;

        Snapshot(Mode mode, long deadlineElapsedRealtime) {
            this.mode = mode;
            this.deadlineElapsedRealtime = deadlineElapsedRealtime;
        }

        long remainingMillis() {
            if (mode == Mode.IDLE) {
                return 0L;
            }
            return Math.max(0L, deadlineElapsedRealtime - SystemClock.elapsedRealtime());
        }
    }

    private static final String PREFS = "removal_state_v1";
    private static final String KEY_MODE = "mode";
    private static final String KEY_DEADLINE = "deadline";
    private static final String KEY_BOOT_COUNT = "boot_count";

    private final Context context;
    private final SharedPreferences preferences;

    RemovalStateStore(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized Snapshot get() {
        Mode mode;
        try {
            mode = Mode.valueOf(preferences.getString(KEY_MODE, Mode.IDLE.name()));
        } catch (IllegalArgumentException invalid) {
            return write(Mode.IDLE, 0L);
        }
        if (preferences.getInt(KEY_BOOT_COUNT, Integer.MIN_VALUE) != currentBootCount()) {
            return write(Mode.IDLE, 0L);
        }
        long deadline = preferences.getLong(KEY_DEADLINE, 0L);
        long now = SystemClock.elapsedRealtime();
        if (mode == Mode.WAITING && now >= deadline) {
            return write(Mode.READY, now + READY_WINDOW_MILLIS);
        }
        if (mode == Mode.READY && now >= deadline) {
            return write(Mode.IDLE, 0L);
        }
        if (mode != Mode.IDLE && deadline <= 0L) {
            return write(Mode.IDLE, 0L);
        }
        return new Snapshot(mode, deadline);
    }

    synchronized Snapshot requestRemoval() {
        Snapshot current = get();
        if (current.mode != Mode.IDLE) {
            return current;
        }
        return write(Mode.WAITING, SystemClock.elapsedRealtime() + WAIT_MILLIS);
    }

    synchronized Snapshot cancel() {
        return write(Mode.IDLE, 0L);
    }

    synchronized boolean removalAllowed() {
        return get().mode == Mode.READY;
    }

    private Snapshot write(Mode mode, long deadline) {
        preferences.edit()
                .putString(KEY_MODE, mode.name())
                .putLong(KEY_DEADLINE, deadline)
                .putInt(KEY_BOOT_COUNT, currentBootCount())
                .commit();
        return new Snapshot(mode, deadline);
    }

    private int currentBootCount() {
        return Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.BOOT_COUNT,
                -1);
    }
}
