package com.glasspb.scoreboard.ui;

import com.glasspb.scoreboard.domain.Mode;
import com.glasspb.scoreboard.domain.Team;
import com.glasspb.scoreboard.input.SemanticGesture;
import com.glasspb.scoreboard.persistence.CheckpointCodec;
import com.glasspb.scoreboard.persistence.MemoryCheckpointStore;

import org.junit.Test;

import static org.junit.Assert.*;

public class NavigationControllerTest {
    @Test
    public void startupWithoutCheckpointBeginsAtModeSelect() {
        ScoreboardController controller = ScoreboardController.start(new MemoryCheckpointStore());

        assertEquals(Screen.MODE_SELECT, controller.getScreen());
    }

    @Test
    public void modeSelectBranchesToServeSelectForSinglesAndDoubles() {
        ScoreboardController controller = ScoreboardController.start(new MemoryCheckpointStore());

        controller.handle(SemanticGesture.BACKWARD);
        assertEquals(Mode.SINGLES, controller.getPendingMode());
        assertEquals(Screen.SERVE_SELECT, controller.getScreen());

        controller.handle(SemanticGesture.DOUBLE_TAP);
        controller.handle(SemanticGesture.FORWARD);
        assertEquals(Mode.DOUBLES, controller.getPendingMode());
        assertEquals(Screen.SERVE_SELECT, controller.getScreen());
    }

    @Test
    public void serveSelectStartsAndPersistsChosenServer() {
        MemoryCheckpointStore store = new MemoryCheckpointStore();
        ScoreboardController controller = ScoreboardController.start(store);
        controller.handle(SemanticGesture.FORWARD);

        controller.handle(SemanticGesture.BACKWARD);

        assertEquals(Screen.PLAYING, controller.getScreen());
        assertEquals(Team.THEM, controller.getState().getServingTeam());
        assertTrue(store.hasCheckpoint());
    }

    @Test
    public void activeCheckpointPromptsResumeThenCanResumeOrDiscard() {
        MemoryCheckpointStore store = new MemoryCheckpointStore();
        ScoreboardController first = ScoreboardController.start(store);
        first.handle(SemanticGesture.FORWARD);
        first.handle(SemanticGesture.FORWARD);

        ScoreboardController resumed = ScoreboardController.start(store);
        assertEquals(Screen.RESUME_PROMPT, resumed.getScreen());

        resumed.handle(SemanticGesture.FORWARD);
        assertEquals(Screen.PLAYING, resumed.getScreen());

        ScoreboardController discard = ScoreboardController.start(store);
        discard.handle(SemanticGesture.BACKWARD);
        assertEquals(Screen.DISCARD_CONFIRM, discard.getScreen());
        discard.handle(SemanticGesture.FORWARD);
        assertEquals(Screen.MODE_SELECT, discard.getScreen());
        assertFalse(store.hasCheckpoint());
    }

    @Test
    public void playingGesturesScoreUndoAndOpenResetConfirmation() {
        ScoreboardController controller = ScoreboardController.start(new MemoryCheckpointStore());
        controller.handle(SemanticGesture.FORWARD);
        controller.handle(SemanticGesture.FORWARD);

        controller.handle(SemanticGesture.FORWARD);
        assertEquals(1, controller.getState().getYouScore());

        controller.handle(SemanticGesture.DOUBLE_TAP);
        assertEquals(0, controller.getState().getYouScore());

        controller.handle(SemanticGesture.LONG_PRESS);
        assertEquals(Screen.RESET_CONFIRM, controller.getScreen());
    }

    @Test
    public void resetRequiresConfirmationAndLongPressAloneNeverDeletes() {
        MemoryCheckpointStore store = new MemoryCheckpointStore();
        ScoreboardController controller = ScoreboardController.start(store);
        controller.handle(SemanticGesture.FORWARD);
        controller.handle(SemanticGesture.FORWARD);
        controller.handle(SemanticGesture.LONG_PRESS);

        controller.handle(SemanticGesture.LONG_PRESS);
        assertTrue(store.hasCheckpoint());
        assertEquals(Screen.RESET_CONFIRM, controller.getScreen());

        controller.handle(SemanticGesture.FORWARD);
        assertFalse(store.hasCheckpoint());
        assertEquals(Screen.MODE_SELECT, controller.getScreen());
    }

    @Test
    public void gameOverDoubleTapUndoesAndForwardStartsNewGameViaModeSelect() {
        MemoryCheckpointStore store = new MemoryCheckpointStore();
        ScoreboardController controller = ScoreboardController.start(store);
        controller.handle(SemanticGesture.FORWARD);
        controller.handle(SemanticGesture.FORWARD);
        controller.setStateForTesting(controller.getState().withScoresForTesting(10, 0));
        controller.handle(SemanticGesture.FORWARD);
        assertEquals(Screen.GAME_OVER, controller.getScreen());

        controller.handle(SemanticGesture.DOUBLE_TAP);
        assertEquals(Screen.PLAYING, controller.getScreen());
        assertEquals(10, controller.getState().getYouScore());

        controller.setStateForTesting(controller.getState().withScoresForTesting(10, 0));
        controller.handle(SemanticGesture.FORWARD);
        controller.handle(SemanticGesture.FORWARD);
        assertEquals(Screen.MODE_SELECT, controller.getScreen());
        assertFalse(store.hasCheckpoint());
    }

    @Test
    public void exitConfirmFinishesWithoutDeletingCheckpoint() {
        MemoryCheckpointStore store = new MemoryCheckpointStore();
        ScoreboardController controller = ScoreboardController.start(store);
        controller.handle(SemanticGesture.FORWARD);
        controller.handle(SemanticGesture.FORWARD);
        controller.setStateForTesting(controller.getState().withScoresForTesting(10, 0));
        controller.handle(SemanticGesture.FORWARD);

        controller.handle(SemanticGesture.LONG_PRESS);
        assertEquals(Screen.EXIT_CONFIRM, controller.getScreen());
        controller.handle(SemanticGesture.FORWARD);

        assertTrue(controller.shouldFinish());
        assertTrue(store.hasCheckpoint());
    }

    @Test
    public void malformedCheckpointRoutesToDiscardFlowWithoutCrash() {
        MemoryCheckpointStore store = new MemoryCheckpointStore();
        store.saveRaw("version=1\nmode=NOPE");

        ScoreboardController controller = ScoreboardController.start(store);

        assertEquals(Screen.DISCARD_CONFIRM, controller.getScreen());
        controller.handle(SemanticGesture.FORWARD);
        assertEquals(Screen.MODE_SELECT, controller.getScreen());
    }
}
