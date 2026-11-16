package com.chandsharma.nodoom;

import android.content.Context;
import android.content.SharedPreferences;

final class RecognitionStore {
    private static final String PREFS = "recognition_health_v1";
    private static final String KEY_LAST_VIEWER = "last_viewer";
    private static final String KEY_LAST_TAB = "last_tab";
    private static final String KEY_LAST_VERSION = "last_version";
    private static final String KEY_WARNING_VERSION = "warning_version";
    private static final String KEY_INTERRUPTED = "interrupted";

    private final SharedPreferences preferences;

    RecognitionStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void recordViewer(String instagramVersion) {
        preferences.edit()
                .putLong(KEY_LAST_VIEWER, System.currentTimeMillis())
                .putString(KEY_LAST_VERSION, instagramVersion)
                .apply();
    }

    void recordReelsTab(String instagramVersion) {
        preferences.edit()
                .putLong(KEY_LAST_TAB, System.currentTimeMillis())
                .putString(KEY_LAST_VERSION, instagramVersion)
                .apply();
    }

    long lastViewerMillis() {
        return preferences.getLong(KEY_LAST_VIEWER, 0L);
    }

    long lastTabMillis() {
        return preferences.getLong(KEY_LAST_TAB, 0L);
    }

    String lastRecognizedVersion() {
        return preferences.getString(KEY_LAST_VERSION, "");
    }

    boolean shouldWarnForVersion(String currentVersion) {
        String lastRecognized = lastRecognizedVersion();
        return currentVersion != null
                && !currentVersion.isBlank()
                && !lastRecognized.isBlank()
                && !currentVersion.equals(lastRecognized)
                && !currentVersion.equals(preferences.getString(KEY_WARNING_VERSION, ""));
    }

    void markVersionWarned(String currentVersion) {
        preferences.edit().putString(KEY_WARNING_VERSION, currentVersion).apply();
    }

    void recordInterruption() {
        preferences.edit().putLong(KEY_INTERRUPTED, System.currentTimeMillis()).apply();
    }

    long lastInterruptionMillis() {
        return preferences.getLong(KEY_INTERRUPTED, 0L);
    }
}

