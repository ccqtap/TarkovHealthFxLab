package com.qq.tarkovhealthfxlab.client;

import java.util.Locale;

public enum BleedingLevel {
    NONE,
    LIGHT,
    HEAVY;

    public BleedingLevel next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static BleedingLevel parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
