package com.chandsharma.nodoom;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

final class AlarmScheduler {
    static final String ACTION_STATE_DEADLINE =
            "com.chandsharma.nodoom.action.STATE_DEADLINE";

    private final Context context;
    private final AlarmManager alarmManager;

    AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = this.context.getSystemService(AlarmManager.class);
    }

    void sync(ProtectionSnapshot snapshot) {
        cancel();
        if (snapshot.mode != ProtectionMode.COUNTDOWN
                && snapshot.mode != ProtectionMode.UNLOCK_WINDOW) {
            return;
        }

        PendingIntent pendingIntent = deadlineIntent(snapshot.mode);
        long triggerAt = Math.max(
                SystemClock.elapsedRealtime() + 1_000L,
                snapshot.deadlineElapsedRealtime);
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent);
        } else {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent);
        }
    }

    void cancel() {
        alarmManager.cancel(deadlineIntent(null));
    }

    private PendingIntent deadlineIntent(ProtectionMode expectedMode) {
        Intent intent = new Intent(context, StateAlarmReceiver.class)
                .setAction(ACTION_STATE_DEADLINE);
        if (expectedMode != null) {
            intent.putExtra(StateAlarmReceiver.EXTRA_EXPECTED_MODE, expectedMode.name());
        }
        return PendingIntent.getBroadcast(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

