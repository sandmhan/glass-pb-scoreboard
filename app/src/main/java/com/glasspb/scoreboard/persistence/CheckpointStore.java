package com.glasspb.scoreboard.persistence;

import com.glasspb.scoreboard.domain.MatchState;

public interface CheckpointStore {
    CheckpointCodec.DecodeResult load();
    boolean save(MatchState state);
    boolean delete();
    boolean hasCheckpoint();
}
