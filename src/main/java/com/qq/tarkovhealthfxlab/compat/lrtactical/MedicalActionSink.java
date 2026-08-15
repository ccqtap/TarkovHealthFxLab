package com.qq.tarkovhealthfxlab.compat.lrtactical;

import net.minecraft.server.level.ServerPlayer;

/**
 * Receives completed LR Tactical medical animations on the logical server.
 * The sink must mutate the authoritative health capability, then reconcile its
 * MobEffect projections. LR has already processed remove_effects (including
 * @harmful) before this callback, so projections cannot be the state source.
 */
@FunctionalInterface
public interface MedicalActionSink {
    MedicalActionSink IGNORE = (player, request) -> {
    };

    void onMedicalAction(ServerPlayer player, MedicalActionRequest request);

    enum MedicalAction {
        REPAIR,
        ANALGESIA
    }

    record MedicalActionRequest(
            MedicalAction action,
            String consumableId,
            boolean lrEffectsAlreadyApplied
    ) {
        public MedicalActionRequest {
            if (action == null) {
                throw new IllegalArgumentException("action cannot be null");
            }
            consumableId = consumableId == null ? "" : consumableId;
        }
    }
}
