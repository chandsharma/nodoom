package com.chandsharma.nodoom;

import android.content.Context;
import android.content.SharedPreferences;

final class UserSettings {
    private static final String PREFS = "user_settings_v2";
    private static final String KEY_UNLOCK_ALERT = "unlock_alert";
    private static final String KEY_COUNTDOWN_STATUS = "countdown_status";
    private static final String KEY_COMPATIBILITY_ALERT = "compatibility_alert";
    private static final String KEY_PROTECTION_STATUS = "protection_status";
    private static final String KEY_BLOCKED_SWIPE_HINT = "blocked_swipe_hint";

    private final SharedPreferences preferences;

    UserSettings(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean unlockAlertEnabled() {
        return preferences.getBoolean(KEY_UNLOCK_ALERT, false);
    }

    void setUnlockAlertEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_UNLOCK_ALERT, enabled).apply();
    }

    boolean countdownStatusEnabled() {
        return preferences.getBoolean(KEY_COUNTDOWN_STATUS, false);
    }

    void setCountdownStatusEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_COUNTDOWN_STATUS, enabled).apply();
    }

    boolean compatibilityAlertEnabled() {
        return preferences.getBoolean(KEY_COMPATIBILITY_ALERT, false);
    }

    void setCompatibilityAlertEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_COMPATIBILITY_ALERT, enabled).apply();
    }

    boolean protectionStatusEnabled() {
        return preferences.getBoolean(KEY_PROTECTION_STATUS, false);
    }

    void setProtectionStatusEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_PROTECTION_STATUS, enabled).apply();
    }

    boolean blockedSwipeHintEnabled() {
        return preferences.getBoolean(KEY_BLOCKED_SWIPE_HINT, false);
    }

    void setBlockedSwipeHintEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_BLOCKED_SWIPE_HINT, enabled).apply();
    }
}
