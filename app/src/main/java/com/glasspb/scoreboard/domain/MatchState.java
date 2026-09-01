package com.glasspb.scoreboard.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MatchState {
    public static final int HISTORY_LIMIT = 32;

    private final Mode mode;
    private final int youScore;
    private final int themScore;
    private final Team servingTeam;
    private final int serverNumber;
    private final Team startingTeam;
    private final boolean openingServe;
    private final boolean gameOver;
    private final Team winner;
    private final Rules rules;
    private final String sessionId;
    private final List<MatchState> history;

    public MatchState(
            Mode mode,
            int youScore,
            int themScore,
            Team servingTeam,
            int serverNumber,
            Team startingTeam,
            boolean openingServe,
            boolean gameOver,
            Team winner,
            Rules rules,
            String sessionId,
            List<MatchState> history) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.youScore = youScore;
        this.themScore = themScore;
        this.servingTeam = Objects.requireNonNull(servingTeam, "servingTeam");
        this.serverNumber = serverNumber;
        this.startingTeam = Objects.requireNonNull(startingTeam, "startingTeam");
        this.openingServe = openingServe;
        this.gameOver = gameOver;
        this.winner = winner;
        this.rules = Objects.requireNonNull(rules, "rules");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.history = Collections.unmodifiableList(new ArrayList<>(history));
    }

    public static MatchState start(Mode mode, Team servingTeam, String sessionId, Rules rules) {
        boolean doubles = mode == Mode.DOUBLES;
        return new MatchState(mode, 0, 0, servingTeam, doubles ? 2 : 0, servingTeam,
                doubles, false, null, rules, sessionId, Collections.<MatchState>emptyList());
    }

    public MatchState withScoresForTesting(int youScore, int themScore) {
        return new MatchState(mode, youScore, themScore, servingTeam, serverNumber, startingTeam,
                openingServe, false, null, rules, sessionId, history);
    }

    MatchState transition(int youScore, int themScore, Team servingTeam, int serverNumber,
                          boolean openingServe, boolean gameOver, Team winner, List<MatchState> history) {
        return new MatchState(mode, youScore, themScore, servingTeam, serverNumber, startingTeam,
                openingServe, gameOver, winner, rules, sessionId, history);
    }

    MatchState snapshotWithoutHistory() {
        return new MatchState(mode, youScore, themScore, servingTeam, serverNumber, startingTeam,
                openingServe, gameOver, winner, rules, sessionId, Collections.<MatchState>emptyList());
    }

    public MatchState withHistoryPublic(List<MatchState> history) {
        return new MatchState(mode, youScore, themScore, servingTeam, serverNumber, startingTeam,
                openingServe, gameOver, winner, rules, sessionId, history);
    }

    public Mode getMode() { return mode; }
    public int getYouScore() { return youScore; }
    public int getThemScore() { return themScore; }
    public Team getServingTeam() { return servingTeam; }
    public int getServerNumber() { return serverNumber; }
    public Team getStartingTeam() { return startingTeam; }
    public boolean isOpeningServe() { return openingServe; }
    public boolean isGameOver() { return gameOver; }
    public Team getWinner() { return winner; }
    public Rules getRules() { return rules; }
    public String getSessionId() { return sessionId; }
    public List<MatchState> getHistory() { return history; }

    public int scoreFor(Team team) {
        return team == Team.YOU ? youScore : themScore;
    }

    public MatchState markScoredForTesting() {
        Team winner = null;
        boolean over = false;
        if (rules.isWinningScore(youScore, themScore)) { over = true; winner = Team.YOU; }
        if (rules.isWinningScore(themScore, youScore)) { over = true; winner = Team.THEM; }
        return transition(youScore, themScore, servingTeam, serverNumber, openingServe, over, winner, history);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchState)) return false;
        MatchState that = (MatchState) o;
        return youScore == that.youScore
                && themScore == that.themScore
                && serverNumber == that.serverNumber
                && openingServe == that.openingServe
                && gameOver == that.gameOver
                && mode == that.mode
                && servingTeam == that.servingTeam
                && startingTeam == that.startingTeam
                && winner == that.winner
                && rules.equals(that.rules)
                && sessionId.equals(that.sessionId)
                && history.equals(that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, youScore, themScore, servingTeam, serverNumber, startingTeam,
                openingServe, gameOver, winner, rules, sessionId, history);
    }
}
