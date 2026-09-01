package com.glasspb.scoreboard.input;

import org.junit.Test;

import static org.junit.Assert.*;

public class RawGestureRecognizerTest {
    @Test
    public void increasingHorizontalSwipeIsForwardByDefault() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);

        assertEquals(SemanticGesture.FORWARD, recognizer.onUp(150, 18, 120));
    }

    @Test
    public void decreasingHorizontalSwipeIsBackward() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(150, 10, 0);

        assertEquals(SemanticGesture.BACKWARD, recognizer.onUp(10, 16, 120));
    }

    @Test
    public void singleTapIsIgnored() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);

        assertEquals(SemanticGesture.NONE, recognizer.onUp(12, 11, 80));
    }

    @Test
    public void closeSecondTapEmitsDoubleTap() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);
        assertEquals(SemanticGesture.NONE, recognizer.onUp(12, 11, 80));
        recognizer.onDown(13, 12, 200);

        assertEquals(SemanticGesture.DOUBLE_TAP, recognizer.onUp(12, 11, 260));
    }

    @Test
    public void longPressEmitsLongPressWhenMovementStaysWithinSlop() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);

        assertEquals(SemanticGesture.LONG_PRESS, recognizer.onUp(11, 10, 800));
    }

    @Test
    public void verticalDominantMotionDoesNotBecomeSwipe() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);

        assertEquals(SemanticGesture.NONE, recognizer.onUp(50, 150, 120));
    }

    @Test
    public void duplicateStreamCannotEmitTwoSemanticActions() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);

        assertEquals(SemanticGesture.FORWARD, recognizer.onUp(150, 10, 100));
        assertEquals(SemanticGesture.NONE, recognizer.onUp(155, 10, 110));
    }

    @Test
    public void moveAwayAndReturnIsNeitherTapNorLongPress() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);
        recognizer.onMove(100, 10, 200);

        assertEquals(SemanticGesture.NONE, recognizer.onUp(11, 10, 800));
    }

    @Test
    public void cancelConsumesActiveStreamAndClearsTapPair() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);
        assertEquals(SemanticGesture.NONE, recognizer.onUp(10, 10, 80));
        recognizer.onDown(10, 10, 160);
        recognizer.onCancel();

        assertEquals(SemanticGesture.NONE, recognizer.onUp(10, 10, 220));
    }

    @Test
    public void tripleTapProducesOneDoubleTapAndStartsNewPair() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);
        assertEquals(SemanticGesture.NONE, recognizer.onUp(10, 10, 50));
        recognizer.onDown(10, 10, 120);
        assertEquals(SemanticGesture.DOUBLE_TAP, recognizer.onUp(10, 10, 170));
        recognizer.onDown(10, 10, 240);

        assertEquals(SemanticGesture.NONE, recognizer.onUp(10, 10, 290));
    }

    @Test
    public void quadTapProducesTwoDoubleTapActions() {
        RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
        recognizer.onDown(10, 10, 0);
        assertEquals(SemanticGesture.NONE, recognizer.onUp(10, 10, 50));
        recognizer.onDown(10, 10, 120);
        assertEquals(SemanticGesture.DOUBLE_TAP, recognizer.onUp(10, 10, 170));
        recognizer.onDown(10, 10, 240);
        assertEquals(SemanticGesture.NONE, recognizer.onUp(10, 10, 290));
        recognizer.onDown(10, 10, 360);

        assertEquals(SemanticGesture.DOUBLE_TAP, recognizer.onUp(10, 10, 410));
    }
}
