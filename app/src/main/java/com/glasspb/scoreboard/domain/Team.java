package com.glasspb.scoreboard.domain;

public enum Team {
    YOU,
    THEM;

    public Team other() {
        return this == YOU ? THEM : YOU;
    }
}
