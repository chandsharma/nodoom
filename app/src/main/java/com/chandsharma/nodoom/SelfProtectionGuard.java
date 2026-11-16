package com.chandsharma.nodoom;

import android.view.accessibility.AccessibilityNodeInfo;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Locale;

final class SelfProtectionGuard {
    private static final int MAX_NODES = 350;

    boolean handlesPackage(String packageName) {
        return "com.android.settings".equals(packageName)
                || "com.google.android.permissioncontroller".equals(packageName);
    }

    boolean shouldClose(
            String packageName,
            AccessibilityNodeInfo root,
            boolean removalAllowed) {
        if (root == null || !handlesPackage(packageName) || removalAllowed) {
            return false;
        }
        String text = visibleText(root);
        if (!text.contains("no doom") && !text.contains("com chandsharma nodoom")) {
            return false;
        }

        if (packageName.equals("com.android.settings")) {
            // Device Admin already blocks ordinary uninstall attempts. Guard only the exact screen
            // that removes that barrier; broad App-info matching previously closed innocent screens.
            return text.contains("device admin")
                    && containsAny(text, "deactivate", "remove device admin");
        }
        if (packageName.equals("com.google.android.permissioncontroller")) {
            return containsAny(text,
                    "uninstall this app", "do you want to uninstall", "delete this app");
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

