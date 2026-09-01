package com.glasspb.scoreboard.persistence;

import com.glasspb.scoreboard.domain.MatchEngine;
import com.glasspb.scoreboard.domain.MatchState;
import com.glasspb.scoreboard.domain.Mode;
import com.glasspb.scoreboard.domain.Rules;
import com.glasspb.scoreboard.domain.Team;

import org.junit.Test;

import static org.junit.Assert.*;

public class CheckpointCodecTest {
    @Test
    public void roundTripsVersionedActiveCheckpointWithHistoryAndRules() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "session-1", Rules.to(15, 2));
        state = MatchEngine.rallyWon(state);
        state = MatchEngine.rallyLost(state);

        String encoded = CheckpointCodec.encode(state);
        CheckpointCodec.DecodeResult decoded = CheckpointCodec.decode(encoded);

        assertEquals(CheckpointStatus.ACTIVE, decoded.getStatus());
        assertEquals(state, decoded.getState());
    }

    @Test
    public void completedCheckpointDecodesAsCompleted() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "session-1", Rules.defaultRules()).withScoresForTesting(10, 0);
        state = MatchEngine.rallyWon(state);

        CheckpointCodec.DecodeResult decoded = CheckpointCodec.decode(CheckpointCodec.encode(state));

        assertEquals(CheckpointStatus.COMPLETED, decoded.getStatus());
        assertTrue(decoded.getState().isGameOver());
    }

    @Test
    public void emptyCheckpointDecodesAsEmpty() {
        CheckpointCodec.DecodeResult decoded = CheckpointCodec.decode("");

        assertEquals(CheckpointStatus.EMPTY, decoded.getStatus());
        assertNull(decoded.getState());
    }

    @Test
    public void malformedCheckpointDecodesAsMalformedWithoutThrowing() {
        CheckpointCodec.DecodeResult decoded = CheckpointCodec.decode("version=1\nmode=BOGUS\ncurrent=not-enough-fields");

        assertEquals(CheckpointStatus.MALFORMED, decoded.getStatus());
        assertNull(decoded.getState());
    }
}
