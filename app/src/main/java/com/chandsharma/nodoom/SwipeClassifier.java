package com.chandsharma.nodoom;

final class SwipeClassifier {
    private SwipeClassifier() {
    }

    static boolean isVertical(float deltaX, float deltaY, float touchSlop) {
        return Math.abs(deltaY) > touchSlop * 1.25f
                && Math.abs(deltaY) > Math.abs(deltaX) * 1.12f;
    }
}

