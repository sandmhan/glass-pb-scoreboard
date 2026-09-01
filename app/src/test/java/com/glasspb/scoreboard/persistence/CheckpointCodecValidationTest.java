package com.glasspb.scoreboard.persistence;

import com.glasspb.scoreboard.domain.MatchEngine;
import com.glasspb.scoreboard.domain.MatchState;
import com.glasspb.scoreboard.domain.Mode;
import com.glasspb.scoreboard.domain.Rules;
import com.glasspb.scoreboard.domain.Team;

import org.junit.Test;

import static org.junit.Assert.*;

public class CheckpointCodecValidationTest {
    @Test
    public void rejectsNegativeScores() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "-1,0,YOU,2,YOU,true,false,NONE", ""));
    }

    @Test
    public void rejectsInvalidBooleanTokensInsteadOfParsingFalse() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "0,0,YOU,2,YOU,yes,false,NONE", ""));
    }

    @Test
    public void rejectsWrongServerNumbersForMode() {
        assertMalformed(raw("s1", "SINGLES", 11, 2, "0,0,YOU,1,YOU,false,false,NONE", ""));
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "0,0,YOU,0,YOU,false,false,NONE", ""));
    }

    @Test
    public void rejectsOpeningServeInSingles() {
        assertMalformed(raw("s1", "SINGLES", 11, 2, "0,0,YOU,0,YOU,true,false,NONE", ""));
    }

    @Test
    public void rejectsDoublesOpeningServeUnlessStartingServerTwoIsStillServing() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "0,0,YOU,1,YOU,true,false,NONE", ""));
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "0,0,THEM,2,YOU,true,false,NONE", ""));
    }

    @Test
    public void rejectsGameOverWithoutWinnerWithoutThrowing() {
        CheckpointCodec.DecodeResult decoded = CheckpointCodec.decode(raw("s1", "DOUBLES", 11, 2, "11,0,YOU,2,YOU,true,true,NONE", ""));

        assertEquals(CheckpointStatus.MALFORMED, decoded.getStatus());
        assertNull(decoded.getState());
    }

    @Test
    public void rejectsActiveStateWithWinner() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "10,0,YOU,2,YOU,true,false,YOU", ""));
    }

    @Test
    public void rejectsCompletedStateThatDoesNotSatisfyWinMargin() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "11,10,YOU,2,YOU,true,true,YOU", ""));
    }

    @Test
    public void rejectsActiveStateAlreadyMeetingWinCondition() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "11,0,YOU,2,YOU,true,false,NONE", ""));
    }

    @Test
    public void rejectsWinnerThatIsNotTheLeader() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "9,11,YOU,2,YOU,true,true,YOU", ""));
    }

    @Test
    public void rejectsOversizedHistory() {
        StringBuilder history = new StringBuilder();
        for (int i = 0; i < MatchState.HISTORY_LIMIT + 1; i++) {
            if (i > 0) history.append(';');
            history.append("0,0,YOU,2,YOU,true,false,NONE");
        }

        assertMalformed(raw("s1", "DOUBLES", 11, 2, "1,0,YOU,2,YOU,true,false,NONE", history.toString()));
    }

    @Test
    public void rejectsEmptyOrUnsafeSessions() {
        assertMalformed(raw("", "DOUBLES", 11, 2, "0,0,YOU,2,YOU,true,false,NONE", ""));
        assertMalformed(raw("../unsafe", "DOUBLES", 11, 2, "0,0,YOU,2,YOU,true,false,NONE", ""));
    }

    @Test
    public void rejectsMalformedDuplicateAndMissingFields() {
        assertMalformed("version=1\nsession=s1\nmode=DOUBLES\ntarget=11\nwinBy=2\ncurrent=0,0,YOU,2,YOU,true,false,NONE\nhistory=\nmode=SINGLES\n");
        assertMalformed("version=1\nsession=s1\nmode=DOUBLES\ntarget=11\nwinBy=2\ncurrent=0,0,YOU,2,YOU,true,false,NONE\nnot-a-field\nhistory=\n");
        assertMalformed("version=1\nsession=s1\nmode=DOUBLES\ntarget=11\nwinBy=2\ncurrent=0,0,YOU,2,YOU,true,false,NONE\n");
    }

    @Test
    public void rejectsInvalidHistorySnapshots() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2, "1,0,YOU,2,YOU,true,false,NONE", "bad-snapshot"));
    }

    @Test
    public void completedWinnerNoneRegressionDoesNotCrash() {
        CheckpointCodec.DecodeResult decoded = CheckpointCodec.decode(raw("s1", "DOUBLES", 11, 2, "11,0,YOU,2,YOU,true,true,NONE", ""));

        assertEquals(CheckpointStatus.MALFORMED, decoded.getStatus());
    }

    @Test
    public void rejectsCompletedStateInUndoHistory() {
        assertMalformed(raw("s1", "SINGLES", 11, 2,
                "10,10,YOU,0,YOU,false,false,NONE",
                "12,10,YOU,0,YOU,false,true,YOU"));
    }

    @Test
    public void rejectsIllegalHistoryToCurrentTransition() {
        assertMalformed(raw("s1", "DOUBLES", 11, 2,
                "7,3,YOU,2,YOU,false,false,NONE",
                "1,0,YOU,1,YOU,false,false,NONE"));
    }

    @Test
    public void rejectsCompletedWinnerWhoWasNotServing() {
        assertMalformed(raw("s1", "SINGLES", 11, 2,
                "12,10,THEM,0,YOU,false,true,YOU", ""));
    }

    @Test
    public void acceptsLegacyTrailingXmlIndentationFromGlassSharedPreferences() {
        String encoded = raw("s1", "DOUBLES", 11, 2,
                "1,0,YOU,2,YOU,true,false,NONE", "");

        CheckpointCodec.DecodeResult decoded = CheckpointCodec.decode(encoded + "    ");

        assertEquals(CheckpointStatus.ACTIVE, decoded.getStatus());
    }

    @Test
    public void checkpointRoundTripsAfterRestart() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "restart-1", Rules.defaultRules());
        state = MatchEngine.rallyWon(state);
        state = MatchEngine.rallyLost(state);

        CheckpointCodec.DecodeResult first = CheckpointCodec.decode(CheckpointCodec.encode(state));
        CheckpointCodec.DecodeResult second = CheckpointCodec.decode(CheckpointCodec.encode(first.getState()));

        assertEquals(CheckpointStatus.ACTIVE, second.getStatus());
        assertEquals(state, second.getState());
    }

    private static void assertMalformed(String raw) {
        CheckpointCodec.DecodeResult decoded = CheckpointCodec.decode(raw);
        assertEquals(CheckpointStatus.MALFORMED, decoded.getStatus());
        assertNull(decoded.getState());
    }

    private static String raw(String session, String mode, int target, int winBy, String current, String history) {
        return "version=1\n"
                + "session=" + session + "\n"
                + "mode=" + mode + "\n"
                + "target=" + target + "\n"
                + "winBy=" + winBy + "\n"
                + "current=" + current + "\n"
                + "history=" + history + "\n";
    }
}
