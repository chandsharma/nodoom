package com.chandsharma.nodoom;

import java.text.Normalizer;
import java.util.Locale;

/** Decides which Reel controls need an immediate modal hand-off instead of gesture replay. */
final class ReelControlPolicy {
    private ReelControlPolicy() {
    }

    static boolean opensModal(CharSequence text, CharSequence contentDescription) {
        String label = normalize(join(text, contentDescription));
        return label.equals("comment")
                || label.startsWith("comment ")
                || label.contains(" view comments")
                || label.equals("share")
                || label.startsWith("share ")
                || label.equals("more")
                || label.startsWith("more ")
                || label.equals("options");
    }

    static boolean shouldRestoreAfterModal(
            boolean protectedViewerBehindModal,
            boolean otherNavigationSelected) {
        return protectedViewerBehindModal && !otherNavigationSelected;
    }

    static boolean shouldProtectDuringModalReturn(
            boolean otherNavigationSelected,
            long nowElapsed,
            long guardUntilElapsed) {
        return !otherNavigationSelected && guardUntilElapsed > nowElapsed;
    }

    private static String join(CharSequence first, CharSequence second) {
        StringBuilder result = new StringBuilder();
        if (first != null) {
            result.append(first);
        }
        if (second != null) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(second);
        }
        return result.toString();
    }

    private static String normalize(CharSequence value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }
}
