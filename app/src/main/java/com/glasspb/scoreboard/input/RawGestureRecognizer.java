package com.glasspb.scoreboard.input;

public final class RawGestureRecognizer {
    private final float swipeDistance;
    private final float dominanceRatio;
    private final float minVelocityPxPerMs;
    private final long doubleTapMs;
    private final long longPressMs;
    private final float tapSlop;

    private boolean active;
    private boolean streamConsumed;
    private boolean movedOutsideTapSlop;
    private float downX;
    private float downY;
    private long downTime;
    private float lastTapX;
    private float lastTapY;
    private long lastTapUpTime = Long.MIN_VALUE;

    private RawGestureRecognizer(float swipeDistance, float dominanceRatio, float minVelocityPxPerMs,
                                 long doubleTapMs, long longPressMs, float tapSlop) {
        this.swipeDistance = swipeDistance;
        this.dominanceRatio = dominanceRatio;
        this.minVelocityPxPerMs = minVelocityPxPerMs;
        this.doubleTapMs = doubleTapMs;
        this.longPressMs = longPressMs;
        this.tapSlop = tapSlop;
    }

    public static RawGestureRecognizer glassDefaults() {
        return new RawGestureRecognizer(80f, 1.5f, 0.3f, 300L, 600L, 30f);
    }

    public void onDown(float x, float y, long timeMs) {
        active = true;
        streamConsumed = false;
        movedOutsideTapSlop = false;
        downX = x;
        downY = y;
        downTime = timeMs;
    }

    public void onMove(float x, float y, long timeMs) {
        if (!active || streamConsumed) return;
        float dx = x - downX;
        float dy = y - downY;
        if (distanceSquared(x, y, downX, downY) > tapSlop * tapSlop) {
            movedOutsideTapSlop = true;
        }
    }

    public void onCancel() {
        active = false;
        streamConsumed = true;
        movedOutsideTapSlop = false;
        lastTapUpTime = Long.MIN_VALUE;
    }

    public SemanticGesture onUp(float x, float y, long timeMs) {
        if (!active || streamConsumed) {
            return SemanticGesture.NONE;
        }
        streamConsumed = true;
        active = false;
        float dx = x - downX;
        float dy = y - downY;
        float absX = Math.abs(dx);
        float absY = Math.abs(dy);
        long duration = Math.max(1L, timeMs - downTime);
        if (absX >= swipeDistance && absX >= absY * dominanceRatio && (absX / duration) >= minVelocityPxPerMs) {
            lastTapUpTime = Long.MIN_VALUE;
            return dx > 0 ? SemanticGesture.FORWARD : SemanticGesture.BACKWARD;
        }
        if (!movedOutsideTapSlop && duration >= longPressMs && absX <= tapSlop && absY <= tapSlop) {
            lastTapUpTime = Long.MIN_VALUE;
            return SemanticGesture.LONG_PRESS;
        }
        if (!movedOutsideTapSlop && duration < longPressMs && absX <= tapSlop && absY <= tapSlop) {
            boolean isDoubleTap = lastTapUpTime != Long.MIN_VALUE
                    && timeMs - lastTapUpTime <= doubleTapMs
                    && distanceSquared(x, y, lastTapX, lastTapY) <= tapSlop * tapSlop;
            if (isDoubleTap) {
                lastTapUpTime = Long.MIN_VALUE;
                return SemanticGesture.DOUBLE_TAP;
            }
            lastTapX = x;
            lastTapY = y;
            lastTapUpTime = timeMs;
            return SemanticGesture.NONE;
        }
        lastTapUpTime = Long.MIN_VALUE;
        return SemanticGesture.NONE;
    }

    private static float distanceSquared(float x1, float y1, float x2, float y2) {
        float x = x1 - x2;
        float y = y1 - y2;
        return x * x + y * y;
    }
}
