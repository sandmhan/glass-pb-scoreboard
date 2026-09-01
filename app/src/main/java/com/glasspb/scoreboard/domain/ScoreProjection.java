package com.glasspb.scoreboard.domain;

public final class ScoreProjection {
    private final Team leftTeam;
    private final int leftScore;
    private final Team rightTeam;
    private final int rightScore;
    private final String serveMarker;

    private ScoreProjection(Team leftTeam, int leftScore, Team rightTeam, int rightScore, String serveMarker) {
        this.leftTeam = leftTeam;
        this.leftScore = leftScore;
        this.rightTeam = rightTeam;
        this.rightScore = rightScore;
        this.serveMarker = serveMarker;
    }

    public static ScoreProjection from(MatchState state) {
        Team left = state.getServingTeam();
        Team right = left.other();
        String marker = state.getMode() == Mode.SINGLES ? "SERVE" : "S" + state.getServerNumber();
        return new ScoreProjection(left, state.scoreFor(left), right, state.scoreFor(right), marker);
    }

    public Team getLeftTeam() { return leftTeam; }
    public int getLeftScore() { return leftScore; }
    public Team getRightTeam() { return rightTeam; }
    public int getRightScore() { return rightScore; }
    public String getServeMarker() { return serveMarker; }
}
