package com.glasspb.scoreboard.ui;

import com.glasspb.scoreboard.domain.MatchEngine;
import com.glasspb.scoreboard.domain.MatchState;
import com.glasspb.scoreboard.domain.Mode;
import com.glasspb.scoreboard.domain.Rules;
import com.glasspb.scoreboard.domain.Team;
import com.glasspb.scoreboard.input.SemanticGesture;
import com.glasspb.scoreboard.persistence.CheckpointCodec;
import com.glasspb.scoreboard.persistence.CheckpointStatus;
import com.glasspb.scoreboard.persistence.CheckpointStore;

import java.util.UUID;

public final class ScoreboardController {
    private final CheckpointStore store;
    private Screen screen;
    private Screen previousScreen;
    private Mode pendingMode = Mode.DOUBLES;
    private MatchState state;
    private MatchState checkpointState;
    private boolean shouldFinish;
    private String feedback = "";
    private boolean persistentFeedback;

    private ScoreboardController(CheckpointStore store) {
        this.store = store;
    }

    public static ScoreboardController start(CheckpointStore store) {
        ScoreboardController controller = new ScoreboardController(store);
        CheckpointCodec.DecodeResult loaded = store.load();
        if (loaded.getStatus() == CheckpointStatus.ACTIVE) {
            controller.checkpointState = loaded.getState();
            controller.screen = Screen.RESUME_PROMPT;
        } else if (loaded.getStatus() == CheckpointStatus.COMPLETED) {
            controller.state = loaded.getState();
            controller.screen = Screen.GAME_OVER;
        } else if (loaded.getStatus() == CheckpointStatus.MALFORMED) {
            controller.screen = Screen.DISCARD_CONFIRM;
            controller.setFeedback("Checkpoint error", true);
        } else {
            controller.screen = Screen.MODE_SELECT;
        }
        return controller;
    }

    public void handle(SemanticGesture gesture) {
        if (gesture == null || gesture == SemanticGesture.NONE) return;
        switch (screen) {
            case MODE_SELECT:
                handleModeSelect(gesture);
                break;
            case SERVE_SELECT:
                handleServeSelect(gesture);
                break;
            case RESUME_PROMPT:
                handleResumePrompt(gesture);
                break;
            case DISCARD_CONFIRM:
                handleDiscardConfirm(gesture);
                break;
            case PLAYING:
                handlePlaying(gesture);
                break;
            case RESET_CONFIRM:
                handleResetConfirm(gesture);
                break;
            case GAME_OVER:
                handleGameOver(gesture);
                break;
            case EXIT_CONFIRM:
                handleExitConfirm(gesture);
                break;
            default:
                break;
        }
    }

    private void handleModeSelect(SemanticGesture gesture) {
        if (gesture == SemanticGesture.BACKWARD || gesture == SemanticGesture.FORWARD) {
            pendingMode = gesture == SemanticGesture.BACKWARD ? Mode.SINGLES : Mode.DOUBLES;
            screen = Screen.SERVE_SELECT;
            setFeedback(pendingMode == Mode.SINGLES ? "Singles" : "Doubles", false);
        } else if (gesture == SemanticGesture.LONG_PRESS) {
            previousScreen = Screen.MODE_SELECT;
            screen = Screen.EXIT_CONFIRM;
        }
    }

    private void handleServeSelect(SemanticGesture gesture) {
        if (gesture == SemanticGesture.DOUBLE_TAP) {
            screen = Screen.MODE_SELECT;
            setFeedback("", false);
            return;
        }
        if (gesture == SemanticGesture.BACKWARD || gesture == SemanticGesture.FORWARD) {
            Team server = gesture == SemanticGesture.BACKWARD ? Team.THEM : Team.YOU;
            MatchState next = MatchState.start(pendingMode, server, UUID.randomUUID().toString(), Rules.defaultRules());
            if (!saveCheckpoint(next)) return;
            state = next;
            screen = Screen.PLAYING;
            setFeedback(server == Team.YOU ? "You serve" : "Them serve", false);
        } else if (gesture == SemanticGesture.LONG_PRESS) {
            previousScreen = Screen.SERVE_SELECT;
            screen = Screen.EXIT_CONFIRM;
        }
    }

    private void handleResumePrompt(SemanticGesture gesture) {
        if (gesture == SemanticGesture.FORWARD) {
            state = checkpointState;
            screen = state != null && state.isGameOver() ? Screen.GAME_OVER : Screen.PLAYING;
            setFeedback("", false);
        } else if (gesture == SemanticGesture.BACKWARD) {
            screen = Screen.DISCARD_CONFIRM;
        } else if (gesture == SemanticGesture.LONG_PRESS) {
            previousScreen = Screen.RESUME_PROMPT;
            screen = Screen.EXIT_CONFIRM;
        }
    }

    private void handleDiscardConfirm(SemanticGesture gesture) {
        if (gesture == SemanticGesture.FORWARD) {
            if (!deleteCheckpoint()) return;
            checkpointState = null;
            state = null;
            screen = Screen.MODE_SELECT;
            setFeedback("", false);
        } else if (gesture == SemanticGesture.BACKWARD) {
            screen = checkpointState == null ? Screen.MODE_SELECT : Screen.RESUME_PROMPT;
            setFeedback("", false);
        } else if (gesture == SemanticGesture.LONG_PRESS) {
            previousScreen = Screen.DISCARD_CONFIRM;
            screen = Screen.EXIT_CONFIRM;
        }
    }

    private void handlePlaying(SemanticGesture gesture) {
        if (gesture == SemanticGesture.FORWARD || gesture == SemanticGesture.BACKWARD) {
            MatchState next = gesture == SemanticGesture.FORWARD ? MatchEngine.rallyWon(state) : MatchEngine.rallyLost(state);
            if (next != state) {
                if (!saveCheckpoint(next)) return;
                state = next;
                screen = state.isGameOver() ? Screen.GAME_OVER : Screen.PLAYING;
                setFeedback(gesture == SemanticGesture.FORWARD ? "Point" : "Serve", false);
            }
        } else if (gesture == SemanticGesture.DOUBLE_TAP) {
            MatchState next = MatchEngine.undo(state);
            if (next != state) {
                if (!saveCheckpoint(next)) return;
                state = next;
                screen = Screen.PLAYING;
                setFeedback("Undo", false);
            }
        } else if (gesture == SemanticGesture.LONG_PRESS) {
            screen = Screen.RESET_CONFIRM;
        }
    }

    private void handleResetConfirm(SemanticGesture gesture) {
        if (gesture == SemanticGesture.BACKWARD) {
            screen = Screen.PLAYING;
            setFeedback("", false);
        } else if (gesture == SemanticGesture.FORWARD) {
            if (!deleteCheckpoint()) return;
            state = null;
            checkpointState = null;
            screen = Screen.MODE_SELECT;
            setFeedback("", false);
        }
    }

    private void handleGameOver(SemanticGesture gesture) {
        if (gesture == SemanticGesture.DOUBLE_TAP) {
            MatchState next = MatchEngine.undo(state);
            if (next != state) {
                if (!saveCheckpoint(next)) return;
                state = next;
                screen = Screen.PLAYING;
                setFeedback("Undo", false);
            }
        } else if (gesture == SemanticGesture.FORWARD) {
            if (!deleteCheckpoint()) return;
            state = null;
            checkpointState = null;
            screen = Screen.MODE_SELECT;
            setFeedback("", false);
        } else if (gesture == SemanticGesture.LONG_PRESS) {
            previousScreen = Screen.GAME_OVER;
            screen = Screen.EXIT_CONFIRM;
        }
    }

    private void handleExitConfirm(SemanticGesture gesture) {
        if (gesture == SemanticGesture.BACKWARD) {
            screen = previousScreen == null ? Screen.MODE_SELECT : previousScreen;
        } else if (gesture == SemanticGesture.FORWARD) {
            shouldFinish = true;
        }
    }

    private boolean saveCheckpoint(MatchState state) {
        if (!store.save(state)) {
            setFeedback("NOT SAVED - RETRY", true);
            return false;
        }
        return true;
    }

    private boolean deleteCheckpoint() {
        if (!store.delete()) {
            setFeedback("DELETE FAILED - RETRY", true);
            return false;
        }
        return true;
    }

    private void setFeedback(String feedback, boolean persistent) {
        this.feedback = feedback;
        this.persistentFeedback = persistent;
    }

    public Screen getScreen() { return screen; }
    public Mode getPendingMode() { return pendingMode; }
    public MatchState getState() { return state; }
    public MatchState getVisibleState() { return state != null ? state : checkpointState; }
    public boolean shouldFinish() { return shouldFinish; }
    public String getFeedback() { return feedback; }

    public void clearTransientFeedback() {
        if (!persistentFeedback) {
            feedback = "";
        }
    }

    public void setStateForTesting(MatchState state) {
        if (saveCheckpoint(state)) {
            this.state = state;
            this.screen = state.isGameOver() ? Screen.GAME_OVER : Screen.PLAYING;
        }
    }
}
