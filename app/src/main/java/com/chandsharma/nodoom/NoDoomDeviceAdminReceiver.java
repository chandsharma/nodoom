package com.chandsharma.nodoom;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public final class NoDoomDeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        ProtectionSnapshot state = new ProtectionStateStore(context).get();
        if (state.mode.isProtected()) {
            return "No Doom is ON. Request to turn it off inside No Doom first; the wait is 60 minutes.";
        }
        return null;
    }
}

