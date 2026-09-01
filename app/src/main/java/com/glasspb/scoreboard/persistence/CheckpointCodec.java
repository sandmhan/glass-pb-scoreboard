package com.glasspb.scoreboard.persistence;

import com.glasspb.scoreboard.domain.MatchEngine;
import com.glasspb.scoreboard.domain.MatchState;
import com.glasspb.scoreboard.domain.Mode;
import com.glasspb.scoreboard.domain.Rules;
import com.glasspb.scoreboard.domain.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CheckpointCodec {
    private static final String VERSION = "1";

    private CheckpointCodec() {}

    public static String encode(MatchState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("version=").append(VERSION).append('\n');
        sb.append("session=").append(state.getSessionId()).append('\n');
        sb.append("mode=").append(state.getMode().name()).append('\n');
        sb.append("target=").append(state.getRules().getTarget()).append('\n');
        sb.append("winBy=").append(state.getRules().getWinBy()).append('\n');
        sb.append("current=").append(encodeSnapshot(state)).append('\n');
        sb.append("history=");
        for (int i = 0; i < state.getHistory().size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(encodeSnapshot(state.getHistory().get(i)));
        }
        return sb.toString();
    }

    public static DecodeResult decode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new DecodeResult(CheckpointStatus.EMPTY, null);
        }
        try {
            Map<String, String> values = parseLines(raw);
            if (!VERSION.equals(values.get("version"))) {
                return new DecodeResult(CheckpointStatus.MALFORMED, null);
            }
            String session = require(values, "session");
            validateSession(session);
            Mode mode = Mode.valueOf(require(values, "mode"));
            Rules rules = Rules.to(parsePositive(values, "target"), parsePositive(values, "winBy"));
            MatchState current = decodeSnapshot(require(values, "current"), mode, rules, session);
            List<MatchState> history = decodeHistory(require(values, "history"), mode, rules, session);
            validateHistory(current, history);
            current = current.withHistoryPublic(history);
            return new DecodeResult(current.isGameOver() ? CheckpointStatus.COMPLETED : CheckpointStatus.ACTIVE, current);
        } catch (RuntimeException ex) {
            return new DecodeResult(CheckpointStatus.MALFORMED, null);
        }
    }

    private static Map<String, String> parseLines(String raw) {
        Map<String, String> values = new HashMap<>();
        int end = raw.length();
        while (end > 0) {
            char c = raw.charAt(end - 1);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                end--;
            } else {
                break;
            }
        }
        String[] lines = raw.substring(0, end).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            if (i == lines.length - 1 && line.length() == 0) {
                continue;
            }
            int index = line.indexOf('=');
            if (index <= 0) throw new IllegalArgumentException("bad line");
            String key = line.substring(0, index);
            if (!isExpectedKey(key)) throw new IllegalArgumentException("unknown key");
            if (values.containsKey(key)) throw new IllegalArgumentException("duplicate key");
            values.put(key, line.substring(index + 1));
        }
        requireAllFields(values);
        return values;
    }

    private static boolean isExpectedKey(String key) {
        return "version".equals(key)
                || "session".equals(key)
                || "mode".equals(key)
                || "target".equals(key)
                || "winBy".equals(key)
                || "current".equals(key)
                || "history".equals(key);
    }

    private static void requireAllFields(Map<String, String> values) {
        require(values, "version");
        require(values, "session");
        require(values, "mode");
        require(values, "target");
        require(values, "winBy");
        require(values, "current");
        require(values, "history");
    }

    private static String require(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) throw new IllegalArgumentException("missing " + key);
        return value;
    }

    private static int parsePositive(Map<String, String> values, String key) {
        int parsed = Integer.parseInt(require(values, key));
        if (parsed < 1) throw new IllegalArgumentException("not positive");
        return parsed;
    }

    private static int parseNonNegative(String value) {
        int parsed = Integer.parseInt(value);
        if (parsed < 0) throw new IllegalArgumentException("negative");
        return parsed;
    }

    private static boolean parseBooleanExact(String value) {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        throw new IllegalArgumentException("bad boolean");
    }

    private static void validateSession(String session) {
        if (session.length() == 0 || session.length() > 128) throw new IllegalArgumentException("bad session");
        if (session.indexOf("..") >= 0 || session.indexOf('/') >= 0 || session.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("unsafe session");
        }
        for (int i = 0; i < session.length(); i++) {
            char c = session.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-'
                    || c == '_'
                    || c == '.';
            if (!allowed) throw new IllegalArgumentException("unsafe session");
        }
    }

    private static String encodeSnapshot(MatchState state) {
        return state.getYouScore() + ","
                + state.getThemScore() + ","
                + state.getServingTeam().name() + ","
                + state.getServerNumber() + ","
                + state.getStartingTeam().name() + ","
                + state.isOpeningServe() + ","
                + state.isGameOver() + ","
                + (state.getWinner() == null ? "NONE" : state.getWinner().name());
    }

    private static List<MatchState> decodeHistory(String raw, Mode mode, Rules rules, String session) {
        List<MatchState> history = new ArrayList<>();
        if (raw.length() == 0) {
            return history;
        }
        String[] snapshots = raw.split(";", -1);
        if (snapshots.length > MatchState.HISTORY_LIMIT) throw new IllegalArgumentException("too much history");
        for (String snapshot : snapshots) {
            if (snapshot.length() == 0) throw new IllegalArgumentException("empty history snapshot");
            history.add(decodeSnapshot(snapshot, mode, rules, session));
        }
        return history;
    }

    private static void validateHistory(MatchState current, List<MatchState> history) {
        MatchState previous = null;
        for (MatchState snapshot : history) {
            if (snapshot.isGameOver()) {
                throw new IllegalArgumentException("completed state in history");
            }
            if (snapshot.getStartingTeam() != current.getStartingTeam()) {
                throw new IllegalArgumentException("history starting team mismatch");
            }
            if (previous != null && !isLegalRallyTransition(previous, snapshot)) {
                throw new IllegalArgumentException("illegal history transition");
            }
            previous = snapshot;
        }
        if (previous != null && !isLegalRallyTransition(previous, current)) {
            throw new IllegalArgumentException("illegal current transition");
        }
    }

    private static boolean isLegalRallyTransition(MatchState before, MatchState after) {
        return sameCoreState(MatchEngine.rallyWon(before), after)
                || sameCoreState(MatchEngine.rallyLost(before), after);
    }

    private static boolean sameCoreState(MatchState first, MatchState second) {
        return first.getMode() == second.getMode()
                && first.getYouScore() == second.getYouScore()
                && first.getThemScore() == second.getThemScore()
                && first.getServingTeam() == second.getServingTeam()
                && first.getServerNumber() == second.getServerNumber()
                && first.getStartingTeam() == second.getStartingTeam()
                && first.isOpeningServe() == second.isOpeningServe()
                && first.isGameOver() == second.isGameOver()
                && first.getWinner() == second.getWinner()
                && first.getRules().equals(second.getRules())
                && first.getSessionId().equals(second.getSessionId());
    }

    private static MatchState decodeSnapshot(String raw, Mode mode, Rules rules, String session) {
        String[] parts = raw.split(",", -1);
        if (parts.length != 8) throw new IllegalArgumentException("bad snapshot");
        int youScore = parseNonNegative(parts[0]);
        int themScore = parseNonNegative(parts[1]);
        Team servingTeam = Team.valueOf(parts[2]);
        int serverNumber = Integer.parseInt(parts[3]);
        Team startingTeam = Team.valueOf(parts[4]);
        boolean openingServe = parseBooleanExact(parts[5]);
        boolean gameOver = parseBooleanExact(parts[6]);
        Team winner = "NONE".equals(parts[7]) ? null : Team.valueOf(parts[7]);

        validateSnapshot(mode, youScore, themScore, servingTeam, serverNumber, startingTeam,
                openingServe, gameOver, winner, rules);
        return new MatchState(
                mode,
                youScore,
                themScore,
                servingTeam,
                serverNumber,
                startingTeam,
                openingServe,
                gameOver,
                winner,
                rules,
                session,
                new ArrayList<MatchState>());
    }

    private static void validateSnapshot(Mode mode, int youScore, int themScore, Team servingTeam,
                                         int serverNumber, Team startingTeam, boolean openingServe,
                                         boolean gameOver, Team winner, Rules rules) {
        if (mode == Mode.SINGLES) {
            if (serverNumber != 0) throw new IllegalArgumentException("bad singles server");
            if (openingServe) throw new IllegalArgumentException("bad singles opening serve");
        } else {
            if (serverNumber != 1 && serverNumber != 2) throw new IllegalArgumentException("bad doubles server");
            if (openingServe && (serverNumber != 2 || servingTeam != startingTeam)) {
                throw new IllegalArgumentException("bad doubles opening serve");
            }
        }

        boolean youWins = rules.isWinningScore(youScore, themScore);
        boolean themWins = rules.isWinningScore(themScore, youScore);
        if (gameOver) {
            if (winner == null) throw new IllegalArgumentException("winner required");
            if (servingTeam != winner) throw new IllegalArgumentException("winner was not serving");
            if (winner == Team.YOU) {
                if (!youWins || themWins || youScore <= themScore) throw new IllegalArgumentException("bad winner");
            } else {
                if (!themWins || youWins || themScore <= youScore) throw new IllegalArgumentException("bad winner");
            }
        } else {
            if (winner != null) throw new IllegalArgumentException("active winner");
            if (youWins || themWins) throw new IllegalArgumentException("active already won");
        }
    }

    public static final class DecodeResult {
        private final CheckpointStatus status;
        private final MatchState state;

        public DecodeResult(CheckpointStatus status, MatchState state) {
            this.status = status;
            this.state = state;
        }

        public CheckpointStatus getStatus() { return status; }
        public MatchState getState() { return state; }
    }
}
