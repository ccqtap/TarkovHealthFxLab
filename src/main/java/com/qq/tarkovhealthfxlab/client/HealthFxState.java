package com.qq.tarkovhealthfxlab.client;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class HealthFxState {
    private final EnumMap<BodyRegion, BodyEffect> effects;
    private final double healthRatio;
    private final boolean painkillerActive;
    private final long revision;

    private HealthFxState(
            Map<BodyRegion, BodyEffect> effects,
            double healthRatio,
            boolean painkillerActive,
            long revision
    ) {
        if (!Double.isFinite(healthRatio) || healthRatio < 0.0D || healthRatio > 1.0D) {
            throw new IllegalArgumentException("healthRatio must be finite and in [0, 1]");
        }
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be nonnegative");
        }
        this.effects = new EnumMap<>(BodyRegion.class);
        for (BodyRegion region : BodyRegion.values()) {
            this.effects.put(region, Objects.requireNonNullElseGet(effects.get(region),
                    () -> BodyEffect.healthy(region.defaultMaximumHealth())));
        }
        this.healthRatio = healthRatio;
        this.painkillerActive = painkillerActive;
        this.revision = revision;
    }

    public static HealthFxState healthy() {
        return restoreFromParts(Map.of(), false, 0L);
    }

    public static HealthFxState restore(
            Map<BodyRegion, BodyEffect> effects,
            double healthRatio,
            boolean painkillerActive,
            long revision
    ) {
        return new HealthFxState(effects, healthRatio, painkillerActive, revision);
    }

    /** Restores regional truth and derives the aggregate ratio from all seven parts. */
    public static HealthFxState restoreFromParts(
            Map<BodyRegion, BodyEffect> effects,
            boolean painkillerActive,
            long revision
    ) {
        EnumMap<BodyRegion, BodyEffect> complete = complete(effects);
        return new HealthFxState(complete, calculateHealthRatio(complete), painkillerActive, revision);
    }

    public BodyEffect effect(BodyRegion region) {
        return this.effects.get(Objects.requireNonNull(region, "region"));
    }

    public Map<BodyRegion, BodyEffect> effects() {
        return Collections.unmodifiableMap(new EnumMap<>(this.effects));
    }

    public double healthRatio() {
        return this.healthRatio;
    }

    public boolean painkillerActive() {
        return this.painkillerActive;
    }

    public long revision() {
        return this.revision;
    }

    public BleedingLevel strongestBleeding() {
        BleedingLevel result = BleedingLevel.NONE;
        for (BodyEffect effect : this.effects.values()) {
            if (effect.bleeding().ordinal() > result.ordinal()) {
                result = effect.bleeding();
            }
        }
        return result;
    }

    public int bleedingPartCount() {
        return (int) this.effects.values().stream()
                .filter(effect -> effect.bleeding() != BleedingLevel.NONE)
                .count();
    }

    public int fractureCount() {
        return (int) this.effects.values().stream().filter(BodyEffect::fractured).count();
    }

    public int blackenedCount() {
        return (int) this.effects.values().stream().filter(BodyEffect::blackened).count();
    }

    public int fracturedArms() {
        return (int) this.effects.entrySet().stream()
                .filter(entry -> entry.getKey().isArm() && entry.getValue().fractured())
                .count();
    }

    public int fracturedLegs() {
        return (int) this.effects.entrySet().stream()
                .filter(entry -> entry.getKey().isLeg() && entry.getValue().fractured())
                .count();
    }

    public int blackenedArms() {
        return (int) this.effects.entrySet().stream()
                .filter(entry -> entry.getKey().isArm() && entry.getValue().blackened())
                .count();
    }

    public int blackenedLegs() {
        return (int) this.effects.entrySet().stream()
                .filter(entry -> entry.getKey().isLeg() && entry.getValue().blackened())
                .count();
    }

    public int impairedArms() {
        return (int) this.effects.entrySet().stream()
                .filter(entry -> entry.getKey().isArm()
                        && (entry.getValue().fractured() || entry.getValue().blackened()))
                .count();
    }

    public int impairedLegs() {
        return (int) this.effects.entrySet().stream()
                .filter(entry -> entry.getKey().isLeg()
                        && (entry.getValue().fractured() || entry.getValue().blackened()))
                .count();
    }

    public double totalCurrentHealth() {
        return this.effects.values().stream().mapToDouble(BodyEffect::currentHealth).sum();
    }

    public double totalMaximumHealth() {
        return this.effects.values().stream().mapToDouble(BodyEffect::maximumHealth).sum();
    }

    public double rawPain() {
        return this.effects.values().stream().mapToDouble(BodyEffect::pain).max().orElse(0.0D);
    }

    public double visiblePain() {
        return this.painkillerActive ? 0.0D : rawPain();
    }

    public boolean active() {
        return this.effects.values().stream().anyMatch(BodyEffect::active) || this.healthRatio < 0.999D;
    }

    public HealthFxState withEffect(BodyRegion region, BodyEffect value) {
        EnumMap<BodyRegion, BodyEffect> copy = new EnumMap<>(this.effects);
        copy.put(Objects.requireNonNull(region, "region"), Objects.requireNonNull(value, "value"));
        return new HealthFxState(copy, calculateHealthRatio(copy), this.painkillerActive, nextRevision());
    }

    public HealthFxState withHealthRatio(double value) {
        return new HealthFxState(this.effects, value, this.painkillerActive, nextRevision());
    }

    public HealthFxState withPainkillerActive(boolean value) {
        return new HealthFxState(this.effects, this.healthRatio, value, nextRevision());
    }

    private long nextRevision() {
        return this.revision == Long.MAX_VALUE ? Long.MAX_VALUE : this.revision + 1L;
    }

    private static EnumMap<BodyRegion, BodyEffect> complete(Map<BodyRegion, BodyEffect> effects) {
        EnumMap<BodyRegion, BodyEffect> complete = new EnumMap<>(BodyRegion.class);
        for (BodyRegion region : BodyRegion.values()) {
            complete.put(region, Objects.requireNonNullElseGet(effects.get(region),
                    () -> BodyEffect.healthy(region.defaultMaximumHealth())));
        }
        return complete;
    }

    private static double calculateHealthRatio(Map<BodyRegion, BodyEffect> effects) {
        double current = 0.0D;
        double maximum = 0.0D;
        for (BodyEffect effect : effects.values()) {
            current += effect.currentHealth();
            maximum += effect.maximumHealth();
        }
        return maximum <= 0.0D ? 1.0D : Math.max(0.0D, Math.min(1.0D, current / maximum));
    }
}
