package com.chandsharma.nodoom;

import android.view.accessibility.AccessibilityNodeInfo;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Locale;

final class SelfProtectionGuard {
    private static final int MAX_NODES = 350;

    boolean shouldClose(String packageName, AccessibilityNodeInfo root) {
        if (root == null || packageName == null) {
            return false;
        }
        String text = visibleText(root);
        if (!text.contains("no doom") && !text.contains("com chandsharma nodoom")) {
            return false;
        }

        if (packageName.equals("com.android.settings")
                || packageName.equals("com.google.android.settings.intelligence")) {
            return containsAny(text,
                    "force stop", "uninstall", "clear storage", "clear cache", "storage cache",
                    "use no doom", "no doom shortcut", "device admin", "deactivate",
                    "battery usage", "allow background usage", "restricted", "app info",
                    "running services", "stop service");
        }
        if (packageName.equals("com.google.android.permissioncontroller")) {
            return containsAny(text, "uninstall", "delete this app", "remove app");
        }
        if (packageName.equals("com.google.android.apps.nexuslauncher")) {
            return containsAny(text, "uninstall", "remove app");
        }
        if (packageName.equals("com.google.android.apps.wellbeing")) {
            return containsAny(text, "app timer", "focus mode", "pause app", "bedtime");
        }
        if (packageName.equals("com.android.vending")) {
            return containsAny(text, "uninstall", "remove");
        }
        return false;
    }

    private String visibleText(AccessibilityNodeInfo root) {
        StringBuilder builder = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        while (!queue.isEmpty() && visited++ < MAX_NODES) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isVisibleToUser()) {
                append(builder, node.getText());
                append(builder, node.getContentDescription());
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    queue.addLast(child);
                }
            }
            if (node != root) {
                node.recycle();
            }
        }
        return normalize(builder);
    }

    private void append(StringBuilder builder, CharSequence value) {
        if (value != null && value.length() > 0) {
            builder.append(' ').append(value);
        }
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(CharSequence value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }
}

