package com.chandsharma.nodoom;

final class ProtectionSnapshot {
    final ProtectionMode mode;
    final long deadlineElapsedRealtime;
    final long displayDeadlineEpochMillis;

    ProtectionSnapshot(
            ProtectionMode mode,
            long deadlineElapsedRealtime,
            long displayDeadlineEpochMillis) {
        this.mode = mode;
        this.deadlineElapsedRealtime = deadlineElapsedRealtime;
        this.displayDeadlineEpochMillis = displayDeadlineEpochMillis;
    }

    long remainingMillis(long elapsedRealtime) {
        if (mode != ProtectionMode.COUNTDOWN && mode != ProtectionMode.UNLOCK_WINDOW) {
            return 0L;
        }
        return Math.max(0L, deadlineElapsedRealtime - elapsedRealtime);
    }
}

