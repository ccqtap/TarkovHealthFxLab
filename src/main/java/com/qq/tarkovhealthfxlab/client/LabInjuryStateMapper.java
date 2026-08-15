package com.qq.tarkovhealthfxlab.client;

import com.qq.tarkovhealthfxlab.common.health.BleedingSeverity;
import com.qq.tarkovhealthfxlab.common.health.BodyPart;
import com.qq.tarkovhealthfxlab.common.health.InjuryState;
import com.qq.tarkovhealthfxlab.network.ClientInjuryState;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Pure projection from the laboratory's synchronized seven-part truth to presentation state. */
public final class LabInjuryStateMapper {
    private LabInjuryStateMapper() {
    }

    public static HealthFxState fromSnapshot(ClientInjuryState.Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return fromTruth(snapshot.state(), snapshot.analgesia());
    }

    public static HealthFxState fromTruth(InjuryState state, boolean analgesia) {
        Objects.requireNonNull(state, "state");
        Map<BodyRegion, BodyEffect> effects = new EnumMap<>(BodyRegion.class);
        for (BodyPart part : BodyPart.values()) {
            BodyRegion region = BodyRegion.valueOf(part.name());
            effects.put(region, new BodyEffect(
                    bleeding(state.bleeding(part)),
                    state.isFractured(part),
                    state.isBlackened(part),
                    state.pain(part),
                    state.health(part),
                    state.maximumHealth(part)
            ));
        }
        return HealthFxState.restoreFromParts(effects, analgesia, revision(state, analgesia));
    }

    private static BleedingLevel bleeding(BleedingSeverity severity) {
        return switch (severity) {
            case NONE -> BleedingLevel.NONE;
            case LIGHT -> BleedingLevel.LIGHT;
            case HEAVY -> BleedingLevel.HEAVY;
        };
    }

    private static long revision(InjuryState state, boolean analgesia) {
        int hash = 31 * state.hashCode() + Boolean.hashCode(analgesia);
        return Integer.toUnsignedLong(hash);
    }
}
