package com.glasspb.scoreboard.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class MatchEngineSinglesUndoProjectionTest {
    @Test
    public void singlesStartsWithoutServerNumberOrOpeningServe() {
        MatchState state = MatchState.start(Mode.SINGLES, Team.THEM, "s1", Rules.defaultRules());

        assertEquals(Mode.SINGLES, state.getMode());
        assertEquals(Team.THEM, state.getServingTeam());
        assertEquals(0, state.getServerNumber());
        assertFalse(state.isOpeningServe());
    }

    @Test
    public void singlesRallyLossSidesOutEveryTime() {
        MatchState state = MatchState.start(Mode.SINGLES, Team.YOU, "s1", Rules.defaultRules());

        MatchState next = MatchEngine.rallyLost(state);

        assertEquals(Team.THEM, next.getServingTeam());
        assertEquals(0, next.getServerNumber());
        assertEquals(0, next.getYouScore());
        assertEquals(0, next.getThemScore());
    }

    @Test
    public void undoRestoresOpeningServeAndServiceSnapshot() {
        MatchState started = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules());
        MatchState sideOut = MatchEngine.rallyLost(started);

        MatchState undone = MatchEngine.undo(sideOut);

        assertEquals(started, undone);
    }

    @Test
    public void undoFromGameOverRestoresPlayableState() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules()).withScoresForTesting(10, 0);
        MatchState winner = MatchEngine.rallyWon(state);

        MatchState undone = MatchEngine.undo(winner);

        assertFalse(undone.isGameOver());
        assertEquals(10, undone.getYouScore());
        assertNull(undone.getWinner());
    }

    @Test
    public void repeatedHistoryIsBoundedToThirtyTwoSnapshots() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.to(100, 2));

        for (int i = 0; i < 40; i++) {
            state = MatchEngine.rallyWon(state);
        }

        assertEquals(32, state.getHistory().size());
        MatchState undone = MatchEngine.undo(state);
        assertEquals(39, undone.getYouScore());
    }

    @Test
    public void noOpUndoDoesNotSaveHistory() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules());

        MatchState undone = MatchEngine.undo(state);

        assertSame(state, undone);
        assertEquals(0, undone.getHistory().size());
    }

    @Test
    public void projectionPlacesServingSideOnLeft() {
        MatchState state = MatchEngine.rallyLost(MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules()));

        ScoreProjection projection = ScoreProjection.from(state);

        assertEquals(Team.THEM, projection.getLeftTeam());
        assertEquals(0, projection.getLeftScore());
        assertEquals(Team.YOU, projection.getRightTeam());
        assertEquals("S1", projection.getServeMarker());
    }

    @Test
    public void singlesProjectionUsesServeMarkerInsteadOfServerNumber() {
        MatchState state = MatchState.start(Mode.SINGLES, Team.THEM, "s1", Rules.defaultRules());

        ScoreProjection projection = ScoreProjection.from(state);

        assertEquals("SERVE", projection.getServeMarker());
    }
}
