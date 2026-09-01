package com.glasspb.scoreboard.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import com.glasspb.scoreboard.domain.MatchState;

public final class AndroidCheckpointStore implements CheckpointStore {
    private static final String PREFS = "scoreboard-checkpoint";
    private static final String KEY = "checkpoint";

    private final SharedPreferences preferences;

    public AndroidCheckpointStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public CheckpointCodec.DecodeResult load() {
        return CheckpointCodec.decode(preferences.getString(KEY, null));
    }

    @Override
    public boolean save(MatchState state) {
        return preferences.edit().putString(KEY, CheckpointCodec.encode(state)).commit();
    }

    @Override
    public boolean delete() {
        return preferences.edit().remove(KEY).commit();
    }

    @Override
    public boolean hasCheckpoint() {
        return preferences.contains(KEY);
    }
}
