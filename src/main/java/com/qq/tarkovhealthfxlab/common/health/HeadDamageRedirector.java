package com.qq.tarkovhealthfxlab.common.health;

import java.util.List;
import java.util.SplittableRandom;

/** Deterministic and unit-testable redirect selection. */
public final class HeadDamageRedirector {
    public static final List<BodyPart> TARGETS = List.of(
            BodyPart.THORAX,
            BodyPart.STOMACH,
            BodyPart.LEFT_ARM,
            BodyPart.RIGHT_ARM,
            BodyPart.LEFT_LEG,
            BodyPart.RIGHT_LEG
    );

    private HeadDamageRedirector() {
    }

    public static BodyPart choose(long seed) {
        return TARGETS.get(new SplittableRandom(seed).nextInt(TARGETS.size()));
    }

    public static DamageApplication resolve(boolean enabled, BodyPart original, float amount, long seed) {
        if (!Float.isFinite(amount) || amount < 0.0F) {
            throw new IllegalArgumentException("damage must be finite and non-negative");
        }
        BodyPart applied = enabled && original == BodyPart.HEAD ? choose(seed) : original;
        return new DamageApplication(original, applied, amount, seed, applied != original);
    }
}
