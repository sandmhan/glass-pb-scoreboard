package com.glasspb.scoreboard.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.glasspb.scoreboard.domain.MatchState;
import com.glasspb.scoreboard.domain.ScoreProjection;

public final class ScoreboardView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ScoreboardController controller;

    public ScoreboardView(Context context) {
        super(context);
        setKeepScreenOn(true);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
    }

    public void setController(ScoreboardController controller) {
        this.controller = controller;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        if (controller == null) return;
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        Screen screen = controller.getScreen();
        switch (screen) {
            case MODE_SELECT:
                title(canvas, "PICKLEBALL");
                text(canvas, "← SINGLES   DOUBLES →", 52, 220);
                break;
            case SERVE_SELECT:
                title(canvas, "WHO SERVES?");
                text(canvas, "← THEM   YOU →", 56, 220);
                hint(canvas, "Double tap: mode");
                break;
            case RESUME_PROMPT:
                title(canvas, "RESUME GAME?");
                drawCanonicalScore(canvas, controller.getVisibleState(), 178);
                hint(canvas, "← discard   resume →");
                break;
            case DISCARD_CONFIRM:
                title(canvas, "DISCARD GAME?");
                hint(canvas, "← cancel   discard →");
                break;
            case PLAYING:
                drawPlaying(canvas);
                break;
            case RESET_CONFIRM:
                title(canvas, "RESET GAME?");
                hint(canvas, "← cancel   reset →");
                break;
            case GAME_OVER:
                drawGameOver(canvas);
                break;
            case EXIT_CONFIRM:
                title(canvas, "EXIT?");
                hint(canvas, "← cancel   exit →");
                break;
            default:
                break;
        }
        drawFeedback(canvas);
    }

    private void drawPlaying(Canvas canvas) {
        MatchState state = controller.getState();
        if (state == null) return;
        ScoreProjection projection = ScoreProjection.from(state);
        paint.setTextAlign(Paint.Align.CENTER);
        int leftX = getWidth() / 4;
        int rightX = (getWidth() * 3) / 4;
        text(canvas, String.valueOf(projection.getLeftScore()), 96, 110, leftX);
        text(canvas, String.valueOf(projection.getRightScore()), 96, 110, rightX);
        text(canvas, projection.getLeftTeam().name(), 32, 164, leftX);
        text(canvas, projection.getRightTeam().name(), 32, 164, rightX);
        paint.setStrokeWidth(5f);
        canvas.drawLine(leftX - 44, 184, leftX + 44, 184, paint);
        text(canvas, projection.getServeMarker(), 40, 255, getWidth() / 2);
        hint(canvas, "← lost   won →   tap-tap undo   hold reset");
    }

    private void drawGameOver(Canvas canvas) {
        MatchState state = controller.getState();
        if (state == null) return;
        String winner = state.getWinner() == null ? "GAME" : state.getWinner().name();
        title(canvas, winner + " WIN");
        text(canvas, state.getYouScore() + " - " + state.getThemScore(), 64, 190);
        hint(canvas, "Double tap: undo   → new game   Hold: exit");
    }

    private void drawCanonicalScore(Canvas canvas, MatchState state, int y) {
        if (state != null) {
            text(canvas, "YOU " + state.getYouScore() + " - " + state.getThemScore() + " THEM", 42, y);
        }
    }

    private void drawFeedback(Canvas canvas) {
        String feedback = controller.getFeedback();
        if (feedback == null || feedback.length() == 0) return;
        paint.setColor(Color.LTGRAY);
        text(canvas, feedback, 20, 338);
        paint.setColor(Color.WHITE);
    }

    private void title(Canvas canvas, String value) {
        text(canvas, value, 54, 118);
    }

    private void hint(Canvas canvas, String value) {
        text(canvas, value, 24, 315);
    }

    private void text(Canvas canvas, String value, int size, int y) {
        text(canvas, value, size, y, getWidth() / 2);
    }

    private void text(Canvas canvas, String value, int size, int y, int x) {
        paint.setTextSize(size);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(value, x, y, paint);
    }
}
