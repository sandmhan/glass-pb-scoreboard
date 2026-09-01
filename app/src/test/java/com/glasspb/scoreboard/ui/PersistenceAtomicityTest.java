package com.glasspb.scoreboard.ui;

import com.glasspb.scoreboard.domain.MatchEngine;
import com.glasspb.scoreboard.domain.MatchState;
import com.glasspb.scoreboard.domain.Mode;
import com.glasspb.scoreboard.domain.Rules;
import com.glasspb.scoreboard.domain.Team;
import com.glasspb.scoreboard.input.SemanticGesture;
import com.glasspb.scoreboard.persistence.CheckpointCodec;
import com.glasspb.scoreboard.persistence.CheckpointStore;
import com.glasspb.scoreboard.persistence.MemoryCheckpointStore;

import org.junit.Test;

import static org.junit.Assert.*;

public class PersistenceAtomicityTest {
    @Test
    public void startSaveFailureStaysOnServeSelectWithNoVisibleStateAndRetryFeedback() {
        ConfigurableCheckpointStore store = new ConfigurableCheckpointStore();
        store.failNextSave();
        ScoreboardController controller = ScoreboardController.start(store);
        controller.handle(SemanticGesture.FORWARD);

        controller.handle(SemanticGesture.FORWARD);

        assertEquals(Screen.SERVE_SELECT, controller.getScreen());
        assertNull(controller.getState());
        assertNull(controller.getVisibleState());
        assertEquals("NOT SAVED - RETRY", controller.getFeedback());
        assertFalse(store.hasCheckpoint());
    }

    @Test
    public void rallySaveFailureKeepsOldStateAndScreenWithPersistentFeedback() {
        ConfigurableCheckpointStore store = new ConfigurableCheckpointStore();
        ScoreboardController controller = startedGame(store);
        MatchState before = controller.getState();
        store.failNextSave();

        controller.handle(SemanticGesture.FORWARD);

        assertSame(before, controller.getState());
        assertEquals(Screen.PLAYING, controller.getScreen());
        assertEquals(0, controller.getState().getYouScore());
        assertEquals("NOT SAVED - RETRY", controller.getFeedback());
        controller.clearTransientFeedback();
        assertEquals("NOT SAVED - RETRY", controller.getFeedback());
    }

    @Test
    public void undoSaveFailureKeepsScoredStateAndScreenWithPersistentFeedback() {
        ConfigurableCheckpointStore store = new ConfigurableCheckpointStore();
        ScoreboardController controller = startedGame(store);
        controller.handle(SemanticGesture.FORWARD);
        MatchState scored = controller.getState();
        store.failNextSave();

        controller.handle(SemanticGesture.DOUBLE_TAP);

        assertSame(scored, controller.getState());
        assertEquals(1, controller.getState().getYouScore());
        assertEquals(Screen.PLAYING, controller.getScreen());
        assertEquals("NOT SAVED - RETRY", controller.getFeedback());
    }

    @Test
    public void resetDeleteFailureKeepsStateAndConfirmationScreen() {
        ConfigurableCheckpointStore store = new ConfigurableCheckpointStore();
        ScoreboardController controller = startedGame(store);
        MatchState before = controller.getState();
        controller.handle(SemanticGesture.LONG_PRESS);
        store.failNextDelete();

        controller.handle(SemanticGesture.FORWARD);

        assertSame(before, controller.getState());
        assertEquals(Screen.RESET_CONFIRM, controller.getScreen());
        assertTrue(store.hasCheckpoint());
        assertEquals("DELETE FAILED - RETRY", controller.getFeedback());
    }

    @Test
    public void discardDeleteFailureKeepsCheckpointAndDiscardConfirmation() {
        ConfigurableCheckpointStore store = new ConfigurableCheckpointStore();
        MatchState checkpoint = MatchState.start(Mode.DOUBLES, Team.YOU, "session-1", Rules.defaultRules());
        store.save(checkpoint);
        ScoreboardController controller = ScoreboardController.start(store);
        controller.handle(SemanticGesture.BACKWARD);
        store.failNextDelete();

        controller.handle(SemanticGesture.FORWARD);

        assertEquals(Screen.DISCARD_CONFIRM, controller.getScreen());
        assertEquals(checkpoint, controller.getVisibleState());
        assertTrue(store.hasCheckpoint());
        assertEquals("DELETE FAILED - RETRY", controller.getFeedback());
    }

    @Test
    public void newGameDeleteFailureKeepsCompletedStateAndGameOverScreen() {
        ConfigurableCheckpointStore store = new ConfigurableCheckpointStore();
        MatchState completed = MatchEngine.rallyWon(MatchState.start(Mode.DOUBLES, Team.YOU, "session-1", Rules.defaultRules()).withScoresForTesting(10, 0));
        store.save(completed);
        ScoreboardController controller = ScoreboardController.start(store);
        store.failNextDelete();

        controller.handle(SemanticGesture.FORWARD);

        assertEquals(Screen.GAME_OVER, controller.getScreen());
        assertEquals(completed, controller.getState());
        assertTrue(store.hasCheckpoint());
        assertEquals("DELETE FAILED - RETRY", controller.getFeedback());
    }

    @Test
    public void noOpGesturesDoNotPersist() {
        ConfigurableCheckpointStore store = new ConfigurableCheckpointStore();
        ScoreboardController controller = startedGame(store);
        int savesAfterStart = store.saveCount;
        int deletesAfterStart = store.deleteCount;

        controller.handle(SemanticGesture.NONE);
        controller.handle(SemanticGesture.DOUBLE_TAP);

        assertEquals(savesAfterStart, store.saveCount);
        assertEquals(deletesAfterStart, store.deleteCount);
    }

    private static ScoreboardController startedGame(ConfigurableCheckpointStore store) {
        ScoreboardController controller = ScoreboardController.start(store);
        controller.handle(SemanticGesture.FORWARD);
        controller.handle(SemanticGesture.FORWARD);
        assertEquals(Screen.PLAYING, controller.getScreen());
        return controller;
    }

    private static final class ConfigurableCheckpointStore implements CheckpointStore {
        private String raw;
        private boolean failNextSave;
        private boolean failNextDelete;
        int saveCount;
        int deleteCount;

        void failNextSave() { failNextSave = true; }
        void failNextDelete() { failNextDelete = true; }

        @Override
        public CheckpointCodec.DecodeResult load() { return CheckpointCodec.decode(raw); }

        @Override
        public boolean save(MatchState state) {
            saveCount++;
            if (failNextSave) {
                failNextSave = false;
                return false;
            }
            raw = CheckpointCodec.encode(state);
            return true;
        }

        @Override
        public boolean delete() {
            deleteCount++;
            if (failNextDelete) {
                failNextDelete = false;
                return false;
            }
            raw = null;
            return true;
        }

        @Override
        public boolean hasCheckpoint() { return raw != null && !raw.isEmpty(); }
    }
}
