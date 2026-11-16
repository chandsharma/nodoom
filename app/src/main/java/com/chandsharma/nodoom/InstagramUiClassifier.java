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
        ORDINARY,
        OTHER
    }

    enum NavigationTarget {
        REELS,
        OTHER,
        NONE
    }

    static final class Result {
        final Screen screen;
        final boolean strongViewerSignature;
        final boolean otherNavigationSelected;
        final int bottomNavigationTop;

        Result(
                Screen screen,
                boolean strongViewerSignature,
                boolean otherNavigationSelected,
                int bottomNavigationTop) {
            this.screen = screen;
            this.strongViewerSignature = strongViewerSignature;
            this.otherNavigationSelected = otherNavigationSelected;
            this.bottomNavigationTop = bottomNavigationTop;
        }
    }

    private static final int MAX_NODES = 700;
    private static final Set<String> MODAL_VIEW_IDS = Set.of(
            InstagramProtectionModule.SHARE_SCROLL_VIEW_ID,
            InstagramProtectionModule.COMMENTS_SCROLL_VIEW_ID,
            InstagramProtectionModule.BOTTOM_SHEET_CONTAINER_VIEW_ID);
    private static final Set<String> NAVIGATION_VIEW_IDS = Set.of(
            InstagramProtectionModule.PACKAGE_NAME + ":id/feed_tab",
            InstagramProtectionModule.PACKAGE_NAME + ":id/clips_tab",
            InstagramProtectionModule.PACKAGE_NAME + ":id/direct_tab",
            InstagramProtectionModule.PACKAGE_NAME + ":id/search_tab",
            InstagramProtectionModule.PACKAGE_NAME + ":id/profile_tab");
    private static final Set<String> ORDINARY_NAVIGATION_VIEW_IDS = Set.of(
            InstagramProtectionModule.PACKAGE_NAME + ":id/feed_tab",
            InstagramProtectionModule.PACKAGE_NAME + ":id/direct_tab",
            InstagramProtectionModule.PACKAGE_NAME + ":id/search_tab",
            InstagramProtectionModule.PACKAGE_NAME + ":id/profile_tab");

    // Instagram keeps "Reels" as a brand name in most locales. These cover known translated
    // labels while geometry remains the primary fallback.
    private static final Set<String> REELS_TOKENS = Set.of(
            "reels", "reel", "릴스", "リール", "reeles", "rolki", "ريلز", "ریلز");
    private static final Set<String> OTHER_NAVIGATION_TOKENS = Set.of(
            "home", "inicio", "accueil", "startseite", "início", "search", "explore",
            "profile", "create", "messages", "notifications", "होम", "главная", "홈",
            "ホーム", "الرئيسية");
    private static final Set<String> SELECTED_TOKENS = Set.of(
            "selected", "seleccionado", "sélectionné", "ausgewählt", "selezionato",
            "selecionado", "выбрано", "선택됨", "選択中", "محدد");

    Result classify(AccessibilityNodeInfo root, int screenWidth, int screenHeight) {
        if (root == null || screenWidth <= 0 || screenHeight <= 0) {
            return new Result(Screen.OTHER, false, false, 0);
        }

        int selectedOrdinaryNavigationTop =
                selectedOrdinaryNavigationTop(root, screenHeight);
        if (selectedOrdinaryNavigationTop > 0) {
            // Exact selected Home/Messages/Explore/Profile tabs are definitive. Querying them by
            // ID avoids both the bounded generic tree walk and transient isVisibleToUser=false
            // reports caused by a full-height inline Home Reel.
            return new Result(
                    Screen.ORDINARY,
                    false,
                    true,
                    selectedOrdinaryNavigationTop);
        }

        List<AccessibilityNodeInfo> nodes = collect(root);
        try {
            List<AccessibilityNodeInfo> bottomButtons = bottomNavigationButtons(
                    nodes, screenWidth, screenHeight);
            AccessibilityNodeInfo reels = findByTokens(bottomButtons, REELS_TOKENS);
            boolean reelsSelected = reels != null && isSelectedDeep(reels, 2);
            boolean otherNavigationSelected = hasOtherSelectedNavigation(bottomButtons, reels);
            int exactBottomNavigationTop = exactBottomNavigationTop(
                    nodes, screenWidth, screenHeight);
            boolean hasBottomNavigation = exactBottomNavigationTop > 0
                    || hasBottomNavigation(bottomButtons, screenWidth, screenHeight);
            int bottomNavigationTop = exactBottomNavigationTop > 0
                    ? exactBottomNavigationTop
                    : hasBottomNavigation ? bottomNavigationTop(bottomButtons) : 0;
            int railCount = verticalActionRailCount(nodes, screenWidth, screenHeight);
            boolean viewerSurface = hasFullscreenScrollSurface(nodes, screenWidth, screenHeight);
            boolean knownModal = hasKnownModal(nodes);
            boolean bottomSheet = hasForegroundBottomSheet(nodes, screenWidth, screenHeight);
            boolean strongViewerSignature = railCount >= 3 && viewerSurface;
            Screen screen = decide(
                    reelsSelected,
                    hasBottomNavigation,
                    railCount,
                    viewerSurface,
                    bottomSheet,
                    knownModal);
            return new Result(
                    screen,
                    strongViewerSignature,
                    otherNavigationSelected,
                    bottomNavigationTop);
        } finally {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != root) {
                    node.recycle();
                }
            }
        }
    }

    int selectedOrdinaryNavigationTop(
            AccessibilityNodeInfo root,
            int screenHeight) {
        int selectedTop = Integer.MAX_VALUE;
        for (String viewId : ORDINARY_NAVIGATION_VIEW_IDS) {
            List<AccessibilityNodeInfo> candidates =
                    root.findAccessibilityNodeInfosByViewId(viewId);
            try {
                for (AccessibilityNodeInfo candidate : candidates) {
                    Rect bounds = new Rect();
                    candidate.getBoundsInScreen(bounds);
                    boolean selected = isSelectedDeep(candidate, 2);
                    if (!bounds.isEmpty()
                            && isSelectedOrdinaryNavigationCandidate(
                            viewId,
                            candidate.isEnabled(),
                            selected,
                            bounds.centerY(),
                            bounds.bottom,
                            screenHeight)) {
                        selectedTop = Math.min(selectedTop, bounds.top);
                    }
                }
            } finally {
                for (AccessibilityNodeInfo candidate : candidates) {
                    candidate.recycle();
                }
            }
        }
        return selectedTop == Integer.MAX_VALUE ? 0 : selectedTop;
    }

    static boolean isSelectedOrdinaryNavigationCandidate(
            String viewId,
            boolean enabled,
            boolean selected,
            int centerY,
            int bottom,
            int screenHeight) {
        return ORDINARY_NAVIGATION_VIEW_IDS.contains(viewId)
                && enabled
                && selected
                && screenHeight > 0
                && centerY >= screenHeight * 0.78f
                && bottom <= screenHeight;
    }

    static Screen decide(
            boolean reelsSelected,
            boolean hasBottomNavigation,
            int verticalRailCount,
            boolean hasViewerSurface,
            boolean hasBottomSheet,
            boolean hasKnownModal) {
        // Instagram reuses sticky_header_list for both Reel comments and the ordinary Home feed.
        // A visible non-Reels tab bar is definitive: its feed, inline videos and sheets must stay
        // native even when one of those reused IDs or Reel-like controls is present.
        if (hasBottomNavigation && !reelsSelected) {
            return Screen.ORDINARY;
        }
        // A foreground comments/share sheet owns the gesture even when the Reels tab remains
        // selected underneath it.
        if (hasKnownModal
                || (hasBottomSheet
                && (reelsSelected || (verticalRailCount >= 3 && hasViewerSurface)))) {
            return Screen.REEL_MODAL;
        }
        if (reelsSelected) {
            return Screen.REELS_TAB;
        }
        // Require both independent signals. A temporarily incomplete Home tree can lose its bottom
        // navigation for a frame; the service confirms this signature twice before gating.
        return verticalRailCount >= 3 && hasViewerSurface
                ? Screen.REEL_VIEWER
                : Screen.OTHER;
    }

    NavigationTarget navigationTarget(AccessibilityNodeInfo node, int screenHeight) {
        if (node == null || screenHeight <= 0) {
            return NavigationTarget.NONE;
        }
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty()
                || bounds.centerY() < screenHeight * 0.78f
                || bounds.height() > screenHeight * 0.22f) {
            return NavigationTarget.NONE;
        }
        String label = normalizedLabel(node);
        for (String token : REELS_TOKENS) {
            if (label.equals(token)
                    || label.startsWith(token + " ")
                    || label.endsWith(" " + token)
                    || label.contains(" " + token + " ")) {
                return NavigationTarget.REELS;
            }
        }
        for (String token : OTHER_NAVIGATION_TOKENS) {
            if (label.equals(token)
                    || label.startsWith(token + " ")
                    || label.endsWith(" " + token)
                    || label.contains(" " + token + " ")) {
                return NavigationTarget.OTHER;
            }
        }
        return NavigationTarget.NONE;
    }

    private List<AccessibilityNodeInfo> collect(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.addLast(root);
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
        while (!queue.isEmpty()) {
            queue.removeFirst().recycle();
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
            String viewId = node.getViewIdResourceName();
            boolean exactNavigationNode = viewId != null
                    && NAVIGATION_VIEW_IDS.contains(viewId);
            // An accessibility overlay can make Instagram transiently report its still-visible
            // tab nodes as not visible-to-user. Exact tab IDs plus bottom-bar geometry remain
            // authoritative; generic candidates still require normal visibility.
            if ((!node.isVisibleToUser() && !exactNavigationNode) || !node.isClickable()) {
                continue;
            }
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty() || !isBottomNavigationCandidate(
                    bounds.centerY(),
                    bounds.width(),
                    bounds.height(),
                    width,
                    height)) {
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

    static boolean isBottomNavigationCandidate(
            int centerY,
            int candidateWidth,
            int candidateHeight,
            int screenWidth,
            int screenHeight) {
        return centerY >= screenHeight * 0.90f
                && candidateWidth <= screenWidth * 0.38f
                && candidateHeight <= screenHeight * 0.22f;
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

    private int bottomNavigationTop(List<AccessibilityNodeInfo> buttons) {
        int top = Integer.MAX_VALUE;
        for (AccessibilityNodeInfo button : buttons) {
            Rect bounds = new Rect();
            button.getBoundsInScreen(bounds);
            top = Math.min(top, bounds.top);
        }
        return top == Integer.MAX_VALUE ? 0 : top;
    }

    private int exactBottomNavigationTop(
            List<AccessibilityNodeInfo> nodes,
            int width,
            int height) {
        Set<String> foundIds = new HashSet<>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int top = Integer.MAX_VALUE;
        for (AccessibilityNodeInfo node : nodes) {
            String viewId = node.getViewIdResourceName();
            if (viewId == null || !NAVIGATION_VIEW_IDS.contains(viewId)) {
                continue;
            }
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty() || !isBottomNavigationCandidate(
                    bounds.centerY(),
                    bounds.width(),
                    bounds.height(),
                    width,
                    height)) {
                continue;
            }
            foundIds.add(viewId);
            minX = Math.min(minX, bounds.centerX());
            maxX = Math.max(maxX, bounds.centerX());
            minY = Math.min(minY, bounds.centerY());
            maxY = Math.max(maxY, bounds.centerY());
            top = Math.min(top, bounds.top);
        }
        if (foundIds.size() < 4
                || maxX - minX < width * 0.55f
                || maxY - minY > height * 0.09f) {
            return 0;
        }
        return top == Integer.MAX_VALUE ? 0 : top;
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

    private boolean isSelectedDeep(AccessibilityNodeInfo node, int remainingDepth) {
        if (isSelected(node)) {
            return true;
        }
        if (remainingDepth <= 0) {
            return false;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            AccessibilityNodeInfo child = node.getChild(index);
            if (child == null) {
                continue;
            }
            try {
                if (isSelectedDeep(child, remainingDepth - 1)) {
                    return true;
                }
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private boolean hasOtherSelectedNavigation(
            List<AccessibilityNodeInfo> bottomButtons,
            AccessibilityNodeInfo reels) {
        for (AccessibilityNodeInfo button : bottomButtons) {
            if (button != reels && isSelectedDeep(button, 2)) {
                return true;
            }
        }
        return false;
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
                    || bounds.height() < height * 0.78f
                    || bounds.top > height * 0.12f
                    || bounds.bottom < height * 0.88f) {
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
            boolean sheetGeometry = bounds.top > height * 0.20f
                    && bounds.bottom > height * 0.80f
                    && bounds.width() > width * 0.72f
                    && bounds.height() < height * 0.78f;
            if (sheetGeometry
                    && (node.isScrollable()
                    || className.contains("recyclerview")
                    || className.contains("listview")
                    || className.contains("gridview"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasKnownModal(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser()) {
                continue;
            }
            String viewId = node.getViewIdResourceName();
            if (viewId != null && MODAL_VIEW_IDS.contains(viewId)) {
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

}
