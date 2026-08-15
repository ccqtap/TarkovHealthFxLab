package com.qq.tarkovhealthfxlab.client;

public record EffectChannels(
        double bleeding,
        double fracture,
        double pain,
        double critical,
        int fracturedArms,
        int fracturedLegs
) {
    public EffectChannels {
        bleeding = clamp01(bleeding);
        fracture = clamp01(fracture);
        pain = clamp01(pain);
        critical = clamp01(critical);
        fracturedArms = Math.max(0, fracturedArms);
        fracturedLegs = Math.max(0, fracturedLegs);
    }

    public static EffectChannels from(HealthFxState state) {
        double bleeding = switch (state.strongestBleeding()) {
            case NONE -> 0.0D;
            case LIGHT -> 0.38D;
            case HEAVY -> 1.0D;
        };
        if (state.bleedingPartCount() > 1) {
            bleeding = Math.min(1.0D, bleeding + 0.12D * (state.bleedingPartCount() - 1));
        }
        return new EffectChannels(
                bleeding,
                Math.min(1.0D, (state.fractureCount() + state.blackenedCount()) / 2.0D),
                state.visiblePain() / 100.0D,
                Math.max(0.0D, (0.45D - state.healthRatio()) / 0.45D),
                state.impairedArms(),
                state.impairedLegs()
        );
    }

    public static EffectChannels interpolate(EffectChannels from, EffectChannels to, double amount) {
        double t = clamp01(amount);
        return new EffectChannels(
                lerp(from.bleeding, to.bleeding, t),
                lerp(from.fracture, to.fracture, t),
                lerp(from.pain, to.pain, t),
                lerp(from.critical, to.critical, t),
                to.fracturedArms,
                to.fracturedLegs
        );
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
