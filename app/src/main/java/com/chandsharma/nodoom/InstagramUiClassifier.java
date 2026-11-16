package com.chandsharma.nodoom;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The only place that knows what Instagram looks like.
 *
 * <p>It deliberately combines geometry, interaction roles and accessibility state instead of
 * captions or usernames. Text tokens are used only to identify Instagram's bottom navigation.
 * Add a new UiProfile here when an Instagram update changes those signals.</p>
 */
final class InstagramUiClassifier {
    enum Screen {
        REELS_TAB,
        REEL_VIEWER,
        REEL_MODAL,
        OTHER
    }

    static final class Result {
        final Screen screen;
        final AccessibilityNodeInfo homeNode;
        final Rect fallbackHomeBounds;

        Result(Screen screen, AccessibilityNodeInfo homeNode, Rect fallbackHomeBounds) {
            this.screen = screen;
            this.homeNode = homeNode;
            this.fallbackHomeBounds = fallbackHomeBounds;
        }

        void recycle() {
            if (homeNode != null) {
                homeNode.recycle();
            }
        }
    }

    private static final int MAX_NODES = 700;

    // Instagram keeps "Reels" as a brand name in most locales. These cover known translated
    // labels while geometry remains the primary fallback.
    private static final Set<String> REELS_TOKENS = Set.of(
            "reels", "reel", "릴스", "リール", "reeles", "rolki", "ريلز", "ریلز");
    private static final Set<String> HOME_TOKENS = Set.of(
            "home", "inicio", "accueil", "startseite", "pagina iniziale", "início",
            "होम", "होम पेज", "главная", "홈", "ホーム", "الرئيسية", "الصفحة الرئيسية");
    private static final Set<String> SELECTED_TOKENS = Set.of(
            "selected", "seleccionado", "sélectionné", "ausgewählt", "selezionato",
            "selecionado", "выбрано", "선택됨", "選択中", "محدد");

    Result classify(AccessibilityNodeInfo root, int screenWidth, int screenHeight) {
        if (root == null || screenWidth <= 0 || screenHeight <= 0) {
            return new Result(Screen.OTHER, null, null);
        }

        List<AccessibilityNodeInfo> nodes = collect(root);
        try {
            List<AccessibilityNodeInfo> bottomButtons = bottomNavigationButtons(
                    nodes, screenWidth, screenHeight);
            AccessibilityNodeInfo home = findByTokens(bottomButtons, HOME_TOKENS);
            AccessibilityNodeInfo reels = findByTokens(bottomButtons, REELS_TOKENS);
            Rect fallbackHome = leftmostBounds(bottomButtons);
            boolean reelsSelected = reels != null && isSelected(reels);
            boolean hasBottomNavigation = hasBottomNavigation(
                    bottomButtons, screenWidth, screenHeight);
            int railCount = verticalActionRailCount(nodes, screenWidth, screenHeight);
            boolean viewerSurface = hasFullscreenScrollSurface(nodes, screenWidth, screenHeight);
            boolean bottomSheet = hasForegroundBottomSheet(nodes, screenWidth, screenHeight);
            Screen screen = decide(
                    reelsSelected,
                    hasBottomNavigation,
                    railCount,
                    viewerSurface,
                    bottomSheet);
            if (screen == Screen.REELS_TAB) {
                return new Result(screen, copy(home),
                        fallbackHome == null ? null : new Rect(fallbackHome));
            }
            return new Result(screen, null, null);
        } finally {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != root) {
                    node.recycle();
                }
            }
        }
    }

    static Screen decide(
            boolean reelsSelected,
            boolean hasBottomNavigation,
            int verticalRailCount,
            boolean hasViewerSurface,
            boolean hasBottomSheet) {
        if (reelsSelected) {
            return Screen.REELS_TAB;
        }
        // Home, Explore and profile screens keep their bottom navigation. Even an inline Reel can
        // expose a right-side action rail, so navigation presence wins over that weaker signal.
        if (hasBottomNavigation) {
            return Screen.OTHER;
        }
        if (verticalRailCount < 3) {
            return Screen.OTHER;
        }
        if (hasBottomSheet) {
            return Screen.REEL_MODAL;
        }
        // Some Instagram builds do not expose their custom pager as scrollable. A full action rail
        // without bottom navigation remains the fallback full-screen viewer signature.
        return hasViewerSurface || verticalRailCount >= 3
                ? Screen.REEL_VIEWER
                : Screen.OTHER;
    }

    boolean isReelsNavigationNode(AccessibilityNodeInfo node, int screenHeight) {
        if (node == null || screenHeight <= 0) {
            return false;
        }
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty() || bounds.centerY() < screenHeight * 0.72f) {
            return false;
        }
        String label = normalizedLabel(node);
        for (String token : REELS_TOKENS) {
            if (label.equals(token)
                    || label.startsWith(token + " ")
                    || label.endsWith(" " + token)
                    || label.contains(" " + token + " ")) {
                return true;
            }
        }
        return false;
    }

    private List<AccessibilityNodeInfo> collect(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        while (!queue.isEmpty() && visited++ < MAX_NODES) {
            AccessibilityNodeInfo node = queue.removeFirst();
            result.add(node);
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    queue.addLast(child);
                }
            }
        }
        return result;
    }

    private List<AccessibilityNodeInfo> bottomNavigationButtons(
            List<AccessibilityNodeInfo> nodes,
            int width,
            int height) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        Set<Integer> centerBuckets = new HashSet<>();
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser() || !node.isClickable()) {
                continue;
            }
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty()
                    || bounds.centerY() < height * 0.78f
                    || bounds.width() > width * 0.38f
                    || bounds.height() > height * 0.22f) {
                continue;
            }
            int bucket = Math.max(0, Math.min(9, bounds.centerX() * 10 / Math.max(1, width)));
            if (centerBuckets.add(bucket)) {
                result.add(node);
            }
        }
        result.sort(Comparator.comparingInt(node -> {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            return bounds.centerX();
        }));
        return result;
    }

    private boolean hasBottomNavigation(
            List<AccessibilityNodeInfo> buttons,
            int width,
            int height) {
        if (buttons.size() < 3 || buttons.size() > 7) {
            return false;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (AccessibilityNodeInfo button : buttons) {
            Rect bounds = new Rect();
            button.getBoundsInScreen(bounds);
            minX = Math.min(minX, bounds.centerX());
            maxX = Math.max(maxX, bounds.centerX());
            minY = Math.min(minY, bounds.centerY());
            maxY = Math.max(maxY, bounds.centerY());
        }
        return maxX - minX >= width * 0.55f
                && maxY - minY <= height * 0.09f;
    }

    private AccessibilityNodeInfo findByTokens(
            List<AccessibilityNodeInfo> nodes,
            Set<String> tokens) {
        for (AccessibilityNodeInfo node : nodes) {
            String label = normalizedLabel(node);
            for (String token : tokens) {
                if (label.equals(token)
                        || label.startsWith(token + " ")
                        || label.contains(" " + token + " ")
                        || label.endsWith(" " + token)) {
                    return node;
                }
            }
        }
        return null;
    }

    private boolean isSelected(AccessibilityNodeInfo node) {
        if (node.isSelected() || node.isChecked()) {
            return true;
        }
        String state = normalize(node.getStateDescription());
        String label = normalizedLabel(node);
        for (String token : SELECTED_TOKENS) {
            if (state.contains(token) || label.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private Rect leftmostBounds(List<AccessibilityNodeInfo> bottomButtons) {
        if (bottomButtons.isEmpty()) {
            return null;
        }
        AccessibilityNodeInfo leftmost = bottomButtons.get(0);
        Rect bounds = new Rect();
        leftmost.getBoundsInScreen(bounds);
        return bounds.isEmpty() ? null : bounds;
    }

    private int verticalActionRailCount(
            List<AccessibilityNodeInfo> nodes,
            int width,
            int height) {
        List<Rect> candidates = new ArrayList<>();
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser() || !node.isClickable()) {
                continue;
            }
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty()
                    || bounds.centerX() < width * 0.64f
                    || bounds.centerY() < height * 0.16f
                    || bounds.centerY() > height * 0.88f
                    || bounds.width() > width * 0.28f
                    || bounds.height() > height * 0.17f) {
                continue;
            }
            candidates.add(bounds);
        }
        if (candidates.size() < 3) {
            return 0;
        }

        int best = 0;
        for (Rect anchor : candidates) {
            int count = 0;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (Rect candidate : candidates) {
                if (Math.abs(candidate.centerX() - anchor.centerX()) <= width * 0.14f) {
                    count++;
                    minY = Math.min(minY, candidate.centerY());
                    maxY = Math.max(maxY, candidate.centerY());
                }
            }
            if (maxY - minY >= height * 0.24f) {
                best = Math.max(best, count);
            }
        }
        return best;
    }

    private boolean hasFullscreenScrollSurface(
            List<AccessibilityNodeInfo> nodes,
            int width,
            int height) {
        for (AccessibilityNodeInfo node : nodes) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty()
                    || bounds.width() < width * 0.72f
                    || bounds.height() < height * 0.56f) {
                continue;
            }
            String className = node.getClassName() == null
                    ? ""
                    : node.getClassName().toString().toLowerCase(Locale.ROOT);
            if (node.isScrollable()
                    || className.contains("recyclerview")
                    || className.contains("viewpager")
                    || node.getActionList().contains(
                    AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasForegroundBottomSheet(
            List<AccessibilityNodeInfo> nodes,
            int width,
            int height) {
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser()) {
                continue;
            }
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty()) {
                continue;
            }
            String className = node.getClassName() == null
                    ? ""
                    : node.getClassName().toString().toLowerCase(Locale.ROOT);
            if (className.contains("edittext") && bounds.centerY() > height * 0.55f) {
                return true;
            }
            boolean sheetGeometry = bounds.top > height * 0.20f
                    && bounds.bottom > height * 0.80f
                    && bounds.width() > width * 0.72f
                    && bounds.height() < height * 0.78f;
            if (sheetGeometry
                    && (node.isScrollable()
                    || className.contains("recyclerview")
                    || className.contains("listview"))) {
                return true;
            }
        }
        return false;
    }

    private String normalizedLabel(AccessibilityNodeInfo node) {
        StringBuilder builder = new StringBuilder();
        append(builder, node.getText());
        append(builder, node.getContentDescription());
        append(builder, node.getStateDescription());
        return normalize(builder);
    }

    private void append(StringBuilder builder, CharSequence value) {
        if (value != null && value.length() > 0) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value);
        }
    }

    private String normalize(CharSequence value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
        return normalized;
    }

    @SuppressWarnings("deprecation")
    private AccessibilityNodeInfo copy(AccessibilityNodeInfo node) {
        return node == null ? null : AccessibilityNodeInfo.obtain(node);
    }
}

