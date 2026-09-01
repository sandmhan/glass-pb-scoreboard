package com.glasspb.scoreboard.domain;

import java.util.ArrayList;
import java.util.List;

public final class MatchEngine {
    private MatchEngine() {}

    public static MatchState rallyWon(MatchState state) {
        if (state.isGameOver()) return state;
        int you = state.getYouScore();
        int them = state.getThemScore();
        if (state.getServingTeam() == Team.YOU) {
            you++;
        } else {
            them++;
        }
        Team winner = null;
        boolean over = false;
        if (state.getRules().isWinningScore(you, them)) {
            over = true;
            winner = Team.YOU;
        } else if (state.getRules().isWinningScore(them, you)) {
            over = true;
            winner = Team.THEM;
        }
        return state.transition(you, them, state.getServingTeam(), state.getServerNumber(),
                state.isOpeningServe(), over, winner, historyWith(state));
    }

    public static MatchState rallyLost(MatchState state) {
        if (state.isGameOver()) return state;
        Team serving = state.getServingTeam();
        int server = state.getServerNumber();
        boolean opening = state.isOpeningServe();
        if (state.getMode() == Mode.SINGLES) {
            serving = serving.other();
            server = 0;
            opening = false;
        } else if (opening) {
            serving = serving.other();
            server = 1;
            opening = false;
        } else if (server == 1) {
            server = 2;
        } else {
            serving = serving.other();
            server = 1;
        }
        return state.transition(state.getYouScore(), state.getThemScore(), serving, server,
                opening, false, null, historyWith(state));
    }

    public static MatchState undo(MatchState state) {
        List<MatchState> history = state.getHistory();
        if (history.isEmpty()) return state;
        List<MatchState> remaining = new ArrayList<>(history.subList(0, history.size() - 1));
        return history.get(history.size() - 1).withHistoryPublic(remaining);
    }

    private static List<MatchState> historyWith(MatchState state) {
        List<MatchState> next = new ArrayList<>(state.getHistory());
        next.add(state.snapshotWithoutHistory());
        while (next.size() > MatchState.HISTORY_LIMIT) {
            next.remove(0);
        }
        return next;
    }
}
