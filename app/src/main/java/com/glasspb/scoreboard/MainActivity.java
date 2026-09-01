package com.glasspb.scoreboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.glasspb.scoreboard.input.RawGestureRecognizer;
import com.glasspb.scoreboard.input.SemanticGesture;
import com.glasspb.scoreboard.persistence.AndroidCheckpointStore;
import com.glasspb.scoreboard.ui.ScoreboardController;
import com.glasspb.scoreboard.ui.ScoreboardView;

public final class MainActivity extends Activity {
    public static final String DEBUG_FORWARD = "com.glasspb.scoreboard.DEBUG_FORWARD";
    public static final String DEBUG_BACKWARD = "com.glasspb.scoreboard.DEBUG_BACKWARD";
    public static final String DEBUG_DOUBLE_TAP = "com.glasspb.scoreboard.DEBUG_DOUBLE_TAP";
    public static final String DEBUG_LONG_PRESS = "com.glasspb.scoreboard.DEBUG_LONG_PRESS";

    private final RawGestureRecognizer recognizer = RawGestureRecognizer.glassDefaults();
    private ScoreboardController controller;
    private ScoreboardView scoreboardView;
    private BroadcastReceiver debugReceiver;
    private long lastMotionEventTime = Long.MIN_VALUE;
    private int lastMotionAction = -1;
    private final Runnable clearFeedback = new Runnable() {
        @Override
        public void run() {
            if (controller != null && scoreboardView != null) {
                controller.clearTransientFeedback();
                scoreboardView.invalidate();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        controller = ScoreboardController.start(new AndroidCheckpointStore(this));
        scoreboardView = new ScoreboardView(this);
        scoreboardView.setController(controller);
        setContentView(scoreboardView);
        registerDebugReceiverIfEnabled();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return handleMotion(event) || super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return handleMotion(event) || super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (BuildConfig.DEBUG && event.getAction() == KeyEvent.ACTION_UP) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                apply(SemanticGesture.FORWARD);
                return true;
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                apply(SemanticGesture.BACKWARD);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean handleMotion(MotionEvent event) {
        int action = event.getActionMasked();
        if (event.getEventTime() == lastMotionEventTime && action == lastMotionAction) {
            return true;
        }
        lastMotionEventTime = event.getEventTime();
        lastMotionAction = action;
        if (action == MotionEvent.ACTION_DOWN) {
            recognizer.onDown(event.getX(), event.getY(), event.getEventTime());
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            recognizer.onMove(event.getX(), event.getY(), event.getEventTime());
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            SemanticGesture gesture = recognizer.onUp(event.getX(), event.getY(), event.getEventTime());
            apply(gesture);
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            recognizer.onCancel();
            return true;
        }
        return false;
    }

    private void apply(SemanticGesture gesture) {
        controller.handle(gesture);
        scoreboardView.removeCallbacks(clearFeedback);
        scoreboardView.postDelayed(clearFeedback, 1200L);
        scoreboardView.invalidate();
        if (controller.shouldFinish()) {
            finish();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerDebugReceiverIfEnabled() {
        if (!BuildConfig.DEBUG) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(DEBUG_FORWARD);
        filter.addAction(DEBUG_BACKWARD);
        filter.addAction(DEBUG_DOUBLE_TAP);
        filter.addAction(DEBUG_LONG_PRESS);
        debugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DEBUG_FORWARD.equals(action)) apply(SemanticGesture.FORWARD);
                else if (DEBUG_BACKWARD.equals(action)) apply(SemanticGesture.BACKWARD);
                else if (DEBUG_DOUBLE_TAP.equals(action)) apply(SemanticGesture.DOUBLE_TAP);
                else if (DEBUG_LONG_PRESS.equals(action)) apply(SemanticGesture.LONG_PRESS);
            }
        };
        // ADB's shell UID has DUMP; ordinary installed applications do not.
        registerReceiver(debugReceiver, filter, android.Manifest.permission.DUMP, null);
    }

    @Override
    protected void onDestroy() {
        if (scoreboardView != null) {
            scoreboardView.removeCallbacks(clearFeedback);
        }
        if (debugReceiver != null) {
            unregisterReceiver(debugReceiver);
            debugReceiver = null;
        }
        super.onDestroy();
    }
}
