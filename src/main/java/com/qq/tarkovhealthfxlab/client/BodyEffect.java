package com.qq.tarkovhealthfxlab.client;

import java.util.Objects;

public record BodyEffect(
        BleedingLevel bleeding,
        boolean fractured,
        boolean blackened,
        double pain,
        double currentHealth,
        double maximumHealth
) {
    public static final BodyEffect NONE = healthy(1.0D);

    /** Keeps the v1 construction surface source-compatible. */
    public BodyEffect(BleedingLevel bleeding, boolean fractured, double pain) {
        this(bleeding, fractured, false, pain, 1.0D, 1.0D);
    }

    public BodyEffect {
        Objects.requireNonNull(bleeding, "bleeding");
        if (!Double.isFinite(pain) || pain < 0.0D || pain > 100.0D) {
            throw new IllegalArgumentException("pain must be finite and in [0, 100]");
        }
        if (!Double.isFinite(maximumHealth) || maximumHealth <= 0.0D) {
            throw new IllegalArgumentException("maximumHealth must be finite and positive");
        }
        if (!Double.isFinite(currentHealth) || currentHealth < 0.0D || currentHealth > maximumHealth) {
            throw new IllegalArgumentException("currentHealth must be finite and in [0, maximumHealth]");
        }
        pain = Math.rint(pain * 10.0D) / 10.0D;
        currentHealth = Math.rint(currentHealth * 10.0D) / 10.0D;
        maximumHealth = Math.rint(maximumHealth * 10.0D) / 10.0D;
    }

    public static BodyEffect healthy(double maximumHealth) {
        return new BodyEffect(BleedingLevel.NONE, false, false, 0.0D,
                maximumHealth, maximumHealth);
    }

    public boolean active() {
        return bleeding != BleedingLevel.NONE || fractured || blackened || pain > 0.0D;
    }

    public BodyEffect withBleeding(BleedingLevel value) {
        return new BodyEffect(value, fractured, blackened, pain, currentHealth, maximumHealth);
    }

    public BodyEffect withFractured(boolean value) {
        return new BodyEffect(bleeding, value, blackened, pain, currentHealth, maximumHealth);
    }

    public BodyEffect withBlackened(boolean value) {
        double health = value ? 0.0D : Math.max(1.0D, currentHealth);
        return new BodyEffect(bleeding, fractured, value, pain, health, maximumHealth);
    }

    public BodyEffect withPain(double value) {
        return new BodyEffect(bleeding, fractured, blackened, value, currentHealth, maximumHealth);
    }

    public BodyEffect withCurrentHealth(double value) {
        double clamped = Math.max(0.0D, Math.min(maximumHealth, value));
        return new BodyEffect(bleeding, fractured, blackened,
                pain, clamped, maximumHealth);
    }

    public BodyEffect withHealth(double current, double maximum) {
        double safeMaximum = Math.max(0.1D, maximum);
        double safeCurrent = Math.max(0.0D, Math.min(safeMaximum, current));
        return new BodyEffect(bleeding, fractured, blackened,
                pain, safeCurrent, safeMaximum);
    }

    public double healthRatio() {
        return currentHealth / maximumHealth;
    }
}
