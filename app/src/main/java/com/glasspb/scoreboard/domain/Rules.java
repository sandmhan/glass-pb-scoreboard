package com.glasspb.scoreboard.domain;

import java.util.Objects;

public final class Rules {
    private final int target;
    private final int winBy;

    private Rules(int target, int winBy) {
        if (target < 1) throw new IllegalArgumentException("target must be positive");
        if (winBy < 1) throw new IllegalArgumentException("winBy must be positive");
        this.target = target;
        this.winBy = winBy;
    }

    public static Rules defaultRules() {
        return to(11, 2);
    }

    public static Rules to(int target, int winBy) {
        return new Rules(target, winBy);
    }

    public int getTarget() {
        return target;
    }

    public int getWinBy() {
        return winBy;
    }

    public boolean isWinningScore(int score, int otherScore) {
        return score >= target && score - otherScore >= winBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rules)) return false;
        Rules rules = (Rules) o;
        return target == rules.target && winBy == rules.winBy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, winBy);
    }
}
