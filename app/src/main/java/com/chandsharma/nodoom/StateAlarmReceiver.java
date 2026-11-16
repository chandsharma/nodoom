package com.chandsharma.nodoom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class StateAlarmReceiver extends BroadcastReceiver {
    static final String EXTRA_EXPECTED_MODE = "expected_mode";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AlarmScheduler.ACTION_STATE_DEADLINE.equals(intent.getAction())) {
            return;
        }

        String expectedMode = intent.getStringExtra(EXTRA_EXPECTED_MODE);
        ProtectionStateStore store = new ProtectionStateStore(context);
        ProtectionSnapshot after = store.get();
        AlarmScheduler scheduler = new AlarmScheduler(context);
        scheduler.sync(after);

        NotificationController notifications = new NotificationController(context);
        if (ProtectionMode.COUNTDOWN.name().equals(expectedMode)
                && after.mode == ProtectionMode.UNLOCK_WINDOW) {
            notifications.showUnlockWindow(after);
        } else if (ProtectionMode.UNLOCK_WINDOW.name().equals(expectedMode)
                && after.mode == ProtectionMode.ON) {
            notifications.showWindowMissed();
        }
        notifications.syncStateNotification(after);
    }
}

