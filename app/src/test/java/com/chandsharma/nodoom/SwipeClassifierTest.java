package com.chandsharma.nodoom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SwipeClassifierTest {
    @Test
    public void blocksUpAndDownSwipes() {
        assertTrue(SwipeClassifier.isVertical(8f, -120f, 16f));
        assertTrue(SwipeClassifier.isVertical(-10f, 140f, 16f));
    }

    @Test
    public void permitsTapsAndHorizontalGestures() {
        assertFalse(SwipeClassifier.isVertical(2f, 3f, 16f));
        assertFalse(SwipeClassifier.isVertical(140f, 20f, 16f));
    }

    @Test
    public void diagonalGestureNeedsClearVerticalIntent() {
        assertFalse(SwipeClassifier.isVertical(90f, 95f, 16f));
        assertTrue(SwipeClassifier.isVertical(70f, 100f, 16f));
    }
}

