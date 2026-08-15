package com.qq.tarkovhealthfxlab.network;

import com.qq.tarkovhealthfxlab.common.health.InjuryState;
import com.qq.tarkovhealthfxlab.common.health.InjuryStateCodec;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side packet cache with no Minecraft client class references. */
public final class ClientInjuryState {
    private static final Map<UUID, Snapshot> STATES = new ConcurrentHashMap<>();

    private ClientInjuryState() {
    }

    static void accept(InjuryStateSyncPacket packet) {
        STATES.put(packet.playerId(), new Snapshot(InjuryStateCodec.decode(packet.encodedState()),
                packet.analgesia(), packet.repair(), packet.regeneration()));
    }

    public static Snapshot get(UUID playerId) {
        Snapshot snapshot = STATES.get(playerId);
        return snapshot == null ? Snapshot.empty() : snapshot.copy();
    }

    public record Snapshot(InjuryState state, boolean analgesia, boolean repair, boolean regeneration) {
        public Snapshot {
            state = state.copy();
        }

        public static Snapshot empty() {
            return new Snapshot(new InjuryState(), false, false, false);
        }

        public Snapshot copy() {
            return new Snapshot(this.state, this.analgesia, this.repair, this.regeneration);
        }
    }
}
