package com.glasspb.scoreboard.persistence;

import com.glasspb.scoreboard.domain.MatchState;

public final class MemoryCheckpointStore implements CheckpointStore {
    private String raw;

    @Override
    public CheckpointCodec.DecodeResult load() {
        return CheckpointCodec.decode(raw);
    }

    @Override
    public boolean save(MatchState state) {
        raw = CheckpointCodec.encode(state);
        return true;
    }

    public void saveRaw(String raw) {
        this.raw = raw;
    }

    @Override
    public boolean delete() {
        raw = null;
        return true;
    }

    @Override
    public boolean hasCheckpoint() {
        return raw != null && !raw.isEmpty();
    }

    public String raw() {
        return raw;
    }
}
