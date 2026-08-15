package com.qq.tarkovhealthfxlab.client;

import java.util.Locale;

public enum BodyRegion {
    HEAD("head", 35.0D),
    THORAX("thorax", 85.0D),
    STOMACH("stomach", 70.0D),
    LEFT_ARM("left_arm", 60.0D),
    RIGHT_ARM("right_arm", 60.0D),
    LEFT_LEG("left_leg", 65.0D),
    RIGHT_LEG("right_leg", 65.0D);

    private final String id;
    private final double defaultMaximumHealth;

    BodyRegion(String id, double defaultMaximumHealth) {
        this.id = id;
        this.defaultMaximumHealth = defaultMaximumHealth;
    }

    public String id() {
        return this.id;
    }

    public double defaultMaximumHealth() {
        return this.defaultMaximumHealth;
    }

    public boolean isArm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    public BodyRegion next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static BodyRegion parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return valueOf(normalized);
    }
}
