package com.qq.tarkovhealthfxlab.client;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public enum HealthFxPreset {
    OFF,
    LIGHT_BLEED,
    HEAVY_BLEED,
    FRACTURE_LEG,
    BLACKENED_ARM,
    BLACKENED_LEG,
    PAIN_HIGH,
    CRITICAL_MIXED,
    ITEM7_SHOWCASE;

    public HealthFxState create(long revision) {
        double healthRatio = 1.0D;
        switch (this) {
            case OFF -> {
            }
            case LIGHT_BLEED -> {
                healthRatio = 0.82D;
            }
            case HEAVY_BLEED -> {
                healthRatio = 0.55D;
            }
            case FRACTURE_LEG -> {
                healthRatio = 0.70D;
            }
            case BLACKENED_ARM -> healthRatio = 0.62D;
            case BLACKENED_LEG -> healthRatio = 0.58D;
            case PAIN_HIGH -> {
                healthRatio = 0.65D;
            }
            case CRITICAL_MIXED -> {
                healthRatio = 0.22D;
            }
            case ITEM7_SHOWCASE -> healthRatio = 0.38D;
        }
        Map<BodyRegion, BodyEffect> effects = baseAtRatio(healthRatio);
        switch (this) {
            case OFF -> {
            }
            case LIGHT_BLEED -> effects.put(BodyRegion.LEFT_ARM,
                    effect(BodyRegion.LEFT_ARM, healthRatio, BleedingLevel.LIGHT, false, false, 12.0D));
            case HEAVY_BLEED -> effects.put(BodyRegion.STOMACH,
                    effect(BodyRegion.STOMACH, healthRatio, BleedingLevel.HEAVY, false, false, 42.0D));
            case FRACTURE_LEG -> effects.put(BodyRegion.LEFT_LEG,
                    effect(BodyRegion.LEFT_LEG, healthRatio, BleedingLevel.NONE, true, false, 48.0D));
            case BLACKENED_ARM -> effects.put(BodyRegion.LEFT_ARM,
                    effect(BodyRegion.LEFT_ARM, 0.0D, BleedingLevel.NONE, false, true, 75.0D));
            case BLACKENED_LEG -> effects.put(BodyRegion.RIGHT_LEG,
                    effect(BodyRegion.RIGHT_LEG, 0.0D, BleedingLevel.NONE, true, true, 85.0D));
            case PAIN_HIGH -> effects.put(BodyRegion.THORAX,
                    effect(BodyRegion.THORAX, healthRatio, BleedingLevel.NONE, false, false, 82.0D));
            case CRITICAL_MIXED -> {
                effects.put(BodyRegion.LEFT_LEG,
                        effect(BodyRegion.LEFT_LEG, 0.0D, BleedingLevel.HEAVY, true, true, 88.0D));
                effects.put(BodyRegion.RIGHT_ARM,
                        effect(BodyRegion.RIGHT_ARM, healthRatio, BleedingLevel.LIGHT, true, false, 64.0D));
            }
            case ITEM7_SHOWCASE -> {
                effects.put(BodyRegion.LEFT_ARM,
                        effect(BodyRegion.LEFT_ARM, 0.0D, BleedingLevel.HEAVY, true, true, 92.0D));
                effects.put(BodyRegion.RIGHT_LEG,
                        effect(BodyRegion.RIGHT_LEG, 0.0D, BleedingLevel.LIGHT, true, true, 80.0D));
                effects.put(BodyRegion.STOMACH,
                        effect(BodyRegion.STOMACH, healthRatio, BleedingLevel.HEAVY, false, false, 70.0D));
            }
        }
        return HealthFxState.restoreFromParts(effects, false, Math.max(0L, revision));
    }

    private static Map<BodyRegion, BodyEffect> baseAtRatio(double ratio) {
        Map<BodyRegion, BodyEffect> effects = new EnumMap<>(BodyRegion.class);
        for (BodyRegion region : BodyRegion.values()) {
            effects.put(region, effect(region, ratio, BleedingLevel.NONE, false, false, 0.0D));
        }
        return effects;
    }

    private static BodyEffect effect(
            BodyRegion region,
            double healthRatio,
            BleedingLevel bleeding,
            boolean fractured,
            boolean blackened,
            double pain
    ) {
        double maximum = region.defaultMaximumHealth();
        return new BodyEffect(bleeding, fractured, blackened, pain,
                blackened ? 0.0D : maximum * healthRatio, maximum);
    }

    public static HealthFxPreset parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
