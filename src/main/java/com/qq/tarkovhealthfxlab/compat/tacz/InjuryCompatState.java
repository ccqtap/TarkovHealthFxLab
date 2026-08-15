package com.qq.tarkovhealthfxlab.compat.tacz;

import java.util.Objects;
import java.util.UUID;

/**
 * Narrow, loader-independent view of the authoritative health state needed by
 * the TaCZ bridge.  Implementations must read the synchronized Tarkov health
 * state, never MobEffect projections.
 */
@FunctionalInterface
public interface InjuryCompatState {
    InjuryCompatState NONE = ignored -> ArmInjuryState.HEALTHY;

    ArmInjuryState armInjuries(UUID playerId);

    default ArmInjuryState safeArmInjuries(UUID playerId) {
        if (playerId == null) {
            return ArmInjuryState.HEALTHY;
        }
        return Objects.requireNonNullElse(armInjuries(playerId), ArmInjuryState.HEALTHY);
    }

    /**
     * Counts are exclusive: a blackened arm belongs in {@code blackenedArms},
     * not in both counters.
     */
    record ArmInjuryState(int fracturedArms, int blackenedArms, boolean analgesiaActive) {
        public static final ArmInjuryState HEALTHY = new ArmInjuryState(0, 0, false);

        public ArmInjuryState {
            fracturedArms = clampArmCount(fracturedArms);
            blackenedArms = clampArmCount(blackenedArms);
            if (fracturedArms + blackenedArms > 2) {
                fracturedArms = 2 - blackenedArms;
            }
        }

        private static int clampArmCount(int value) {
            return Math.max(0, Math.min(2, value));
        }
    }
}
