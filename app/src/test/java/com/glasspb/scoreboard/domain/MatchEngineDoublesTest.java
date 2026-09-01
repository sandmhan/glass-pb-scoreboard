package com.glasspb.scoreboard.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class MatchEngineDoublesTest {
    @Test
    public void doublesStartsAtZeroZeroWithOpeningServerTwo() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules());

        assertEquals(0, state.getYouScore());
        assertEquals(0, state.getThemScore());
        assertEquals(Team.YOU, state.getServingTeam());
        assertEquals(2, state.getServerNumber());
        assertTrue(state.isOpeningServe());
        assertFalse(state.isGameOver());
    }

    @Test
    public void servingSideWinAwardsPointAndRetainsService() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules());

        MatchState next = MatchEngine.rallyWon(state);

        assertEquals(1, next.getYouScore());
        assertEquals(0, next.getThemScore());
        assertEquals(Team.YOU, next.getServingTeam());
        assertEquals(2, next.getServerNumber());
        assertEquals(1, next.getHistory().size());
    }

    @Test
    public void openingServerLossImmediatelySidesOutToServerOne() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules());

        MatchState next = MatchEngine.rallyLost(state);

        assertEquals(0, next.getYouScore());
        assertEquals(0, next.getThemScore());
        assertEquals(Team.THEM, next.getServingTeam());
        assertEquals(1, next.getServerNumber());
        assertFalse(next.isOpeningServe());
    }

    @Test
    public void normalServerOneLossAdvancesToServerTwoWithoutSideOut() {
        MatchState afterOpeningSideOut = MatchEngine.rallyLost(MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules()));

        MatchState next = MatchEngine.rallyLost(afterOpeningSideOut);

        assertEquals(Team.THEM, next.getServingTeam());
        assertEquals(0, next.getThemScore());
        assertEquals(0, next.getYouScore());
        assertEquals(2, next.getServerNumber());
    }

    @Test
    public void normalServerTwoLossTransfersServeToOtherTeamServerOne() {
        MatchState s1 = MatchEngine.rallyLost(MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules()));
        MatchState s2 = MatchEngine.rallyLost(s1);

        MatchState next = MatchEngine.rallyLost(s2);

        assertEquals(Team.YOU, next.getServingTeam());
        assertEquals(1, next.getServerNumber());
        assertEquals(0, next.getYouScore());
        assertEquals(0, next.getThemScore());
    }

    @Test
    public void detectsWinAtElevenWithTwoPointMargin() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules());
        for (int i = 0; i < 10; i++) {
            state = MatchEngine.rallyWon(state);
        }

        MatchState winner = MatchEngine.rallyWon(state);

        assertTrue(winner.isGameOver());
        assertEquals(Team.YOU, winner.getWinner());
    }

    @Test
    public void deuceRequiresTwoPointMargin() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules());
        state = state.withScoresForTesting(10, 10);

        MatchState elevenTen = MatchEngine.rallyWon(state);

        assertFalse(elevenTen.isGameOver());
    }

    @Test
    public void scoringAfterGameOverIsRejectedWithoutSavingHistory() {
        MatchState state = MatchState.start(Mode.DOUBLES, Team.YOU, "s1", Rules.defaultRules()).withScoresForTesting(10, 0);
        MatchState winner = MatchEngine.rallyWon(state);

        MatchState afterWinAttempt = MatchEngine.rallyWon(winner);

        assertSame(winner, afterWinAttempt);
    }
}
