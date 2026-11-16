package com.chandsharma.nodoom;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.format.DateFormat;

import java.util.Date;

final class NotificationController {
    private static final String CHANNEL_STATUS = "protection_status";
    private static final String CHANNEL_ALERTS = "unlock_alerts";
    private static final String CHANNEL_GUARD = "self_protection";

    private static final int ID_COUNTDOWN = 201;
    private static final int ID_WINDOW = 202;
    private static final int ID_MISSED = 203;
    private static final int ID_GUARD = 204;
    private static final int ID_COMPATIBILITY = 205;

    private final Context context;
    private final NotificationManager manager;

    NotificationController(Context context) {
        this.context = context.getApplicationContext();
        this.manager = this.context.getSystemService(NotificationManager.class);
        createChannels();
    }

    void syncStateNotification(ProtectionSnapshot snapshot) {
        if (!canNotify()) {
            return;
        }
        if (snapshot.mode != ProtectionMode.COUNTDOWN) {
            manager.cancel(ID_COUNTDOWN);
            return;
        }

        String time = DateFormat.getTimeFormat(context)
                .format(new Date(snapshot.displayDeadlineEpochMillis));
        Notification notification = new Notification.Builder(context, CHANNEL_STATUS)
                .setSmallIcon(com.chandsharma.nodoom.R.drawable.ic_notification)
                .setContentTitle("No Doom stays on")
                .setContentText("No Doom can be turned off at " + time + ". Tap to cancel.")
                .setContentIntent(openAppIntent())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .build();
        manager.notify(ID_COUNTDOWN, notification);
    }

    void showUnlockWindow(ProtectionSnapshot snapshot) {
        if (!canNotify()) {
            return;
        }
        manager.cancel(ID_COUNTDOWN);
        Notification notification = new Notification.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(com.chandsharma.nodoom.R.drawable.ic_notification)
                .setContentTitle("Your unlock window is open")
                .setContentText("You can turn off No Doom in the next 10 minutes.")
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();
        manager.notify(ID_WINDOW, notification);
    }

    void showWindowMissed() {
        if (!canNotify()) {
            return;
        }
        manager.cancel(ID_WINDOW);
        Notification notification = new Notification.Builder(context, CHANNEL_STATUS)
                .setSmallIcon(com.chandsharma.nodoom.R.drawable.ic_notification)
                .setContentTitle("Unlock window missed")
                .setContentText("No Doom stays ON. A new request starts from 60 minutes.")
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .build();
        manager.notify(ID_MISSED, notification);
    }

    void showGuardedScreenMessage() {
        if (!canNotify()) {
            return;
        }
        Notification notification = new Notification.Builder(context, CHANNEL_GUARD)
                .setSmallIcon(com.chandsharma.nodoom.R.drawable.ic_notification)
                .setContentTitle("No Doom is protecting you")
                .setContentText("To turn it off, request it in No Doom and wait 60 minutes.")
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .addAction(new Notification.Action.Builder(
                        null,
                        "Open No Doom",
                        openAppIntent()).build())
                .build();
        manager.notify(ID_GUARD, notification);
    }

    void showInstagramCompatibilityWarning(String versionName) {
        if (!canNotify()) {
            return;
        }
        Notification notification = new Notification.Builder(context, CHANNEL_STATUS)
                .setSmallIcon(com.chandsharma.nodoom.R.drawable.ic_notification)
                .setContentTitle("Instagram changed")
                .setContentText("Instagram " + versionName
                        + " has not been recognised yet. No Doom may need an update.")
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .build();
        manager.notify(ID_COMPATIBILITY, notification);
    }

    private void createChannels() {
        NotificationChannel status = new NotificationChannel(
                CHANNEL_STATUS,
                "Protection status",
                NotificationManager.IMPORTANCE_LOW);
        status.setDescription("Silent countdown and compatibility notices");
        status.setSound(null, null);

        NotificationChannel alerts = new NotificationChannel(
                CHANNEL_ALERTS,
                "Unlock window",
                NotificationManager.IMPORTANCE_DEFAULT);
        alerts.setDescription("Alerts when the 10-minute unlock window opens");

        NotificationChannel guard = new NotificationChannel(
                CHANNEL_GUARD,
                "Self-protection",
                NotificationManager.IMPORTANCE_HIGH);
        guard.setDescription("Shown only after a protected Settings or uninstall screen is closed");

        manager.createNotificationChannel(status);
        manager.createNotificationChannel(alerts);
        manager.createNotificationChannel(guard);
    }

    private PendingIntent openAppIntent() {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                301,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private boolean canNotify() {
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}

