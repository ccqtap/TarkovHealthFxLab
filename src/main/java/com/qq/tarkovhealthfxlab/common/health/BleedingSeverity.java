package com.qq.tarkovhealthfxlab.common.health;

import java.util.Locale;

public enum BleedingSeverity {
    NONE,
    LIGHT,
    HEAVY;

    public static BleedingSeverity parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
