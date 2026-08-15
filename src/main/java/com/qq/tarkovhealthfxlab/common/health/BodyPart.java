package com.qq.tarkovhealthfxlab.common.health;

import java.util.Locale;

/** Seven-part health layout used by the laboratory server state. */
public enum BodyPart {
    HEAD("head", 35.0F),
    THORAX("thorax", 85.0F),
    STOMACH("stomach", 70.0F),
    LEFT_ARM("left_arm", 60.0F),
    RIGHT_ARM("right_arm", 60.0F),
    LEFT_LEG("left_leg", 65.0F),
    RIGHT_LEG("right_leg", 65.0F);

    private final String id;
    private final float defaultMaximum;

    BodyPart(String id, float defaultMaximum) {
        this.id = id;
        this.defaultMaximum = defaultMaximum;
    }

    public String id() {
        return this.id;
    }

    public float defaultMaximum() {
        return this.defaultMaximum;
    }

    public boolean isArm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    public boolean isLimb() {
        return isArm() || isLeg();
    }

    public static BodyPart parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
