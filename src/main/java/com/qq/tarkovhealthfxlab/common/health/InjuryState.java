package com.qq.tarkovhealthfxlab.common.health;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Pure seven-part health truth. Mob effects are deliberately not stored here. */
public final class InjuryState {
    public static final float MAX_PAIN = 100.0F;

    private final float[] health;
    private final float[] maximumHealth;
    private final BleedingSeverity[] bleeding;
    private final float[] pain;
    private final EnumSet<BodyPart> fractures;
    private final EnumSet<BodyPart> blackened;
    private BodyPart lastAffectedPart;

    public InjuryState() {
        int size = BodyPart.values().length;
        this.health = new float[size];
        this.maximumHealth = new float[size];
        this.bleeding = new BleedingSeverity[size];
        this.pain = new float[size];
        for (BodyPart part : BodyPart.values()) {
            this.maximumHealth[part.ordinal()] = part.defaultMaximum();
            this.health[part.ordinal()] = part.defaultMaximum();
            this.bleeding[part.ordinal()] = BleedingSeverity.NONE;
        }
        this.fractures = EnumSet.noneOf(BodyPart.class);
        this.blackened = EnumSet.noneOf(BodyPart.class);
        this.lastAffectedPart = BodyPart.STOMACH;
    }

    private InjuryState(InjuryState source) {
        this.health = source.health.clone();
        this.maximumHealth = source.maximumHealth.clone();
        this.bleeding = source.bleeding.clone();
        this.pain = source.pain.clone();
        this.fractures = source.fractures.clone();
        this.blackened = source.blackened.clone();
        this.lastAffectedPart = source.lastAffectedPart;
    }

    public InjuryState copy() {
        return new InjuryState(this);
    }

    public BodyPart lastAffectedPart() {
        return this.lastAffectedPart;
    }

    public void setLastAffectedPart(BodyPart part) {
        this.lastAffectedPart = Objects.requireNonNull(part, "part");
    }

    public float health(BodyPart part) {
        return this.health[part.ordinal()];
    }

    public float maximumHealth(BodyPart part) {
        return this.maximumHealth[part.ordinal()];
    }

    public void setMaximumHealth(BodyPart part, float value) {
        requireFinitePositive(value, "maximum health");
        int index = part.ordinal();
        this.maximumHealth[index] = value;
        setHealth(part, Math.min(this.health[index], value));
    }

    public void setHealth(BodyPart part, float value) {
        requireFinite(value, "health");
        int index = part.ordinal();
        float previous = this.health[index];
        this.health[index] = clamp(value, 0.0F, this.maximumHealth[index]);
        if (this.health[index] < previous) {
            this.lastAffectedPart = part;
            ensurePain(part, Math.min(MAX_PAIN, 10.0F + (previous - this.health[index])));
        }
        if (part.isLimb() && this.health[index] <= 0.0F) {
            this.blackened.add(part);
            ensurePain(part, 75.0F);
        }
    }

    public float damage(BodyPart part, float amount) {
        requireFiniteNonNegative(amount, "damage");
        float beforeTotal = totalHealth();
        float direct = Math.min(amount, health(part));
        setHealth(part, health(part) - direct);
        float excess = amount - direct;
        float overflow = excess * overflowMultiplier(part);
        if (overflow > 0.0F) {
            distributeOverflow(part, overflow);
        }
        this.lastAffectedPart = part;
        return beforeTotal - totalHealth();
    }

    /** Returns the amount actually healed. */
    public float heal(BodyPart part, float amount, HealingSource source) {
        requireFiniteNonNegative(amount, "healing");
        Objects.requireNonNull(source, "source");
        if (this.blackened.contains(part)) {
            if (!source.unlocksBlackenedLimb()) {
                return 0.0F;
            }
            this.blackened.remove(part);
            this.health[part.ordinal()] = Math.max(1.0F, this.health[part.ordinal()]);
        }
        float before = health(part);
        this.health[part.ordinal()] = clamp(before + amount, 0.0F, maximumHealth(part));
        this.lastAffectedPart = part;
        return health(part) - before;
    }

    /** Highest active level; useful for the single vanilla projection icon. */
    public BleedingSeverity bleeding() {
        BleedingSeverity result = BleedingSeverity.NONE;
        for (BleedingSeverity value : this.bleeding) {
            if (value.ordinal() > result.ordinal()) {
                result = value;
            }
        }
        return result;
    }

    public BleedingSeverity bleeding(BodyPart part) {
        return this.bleeding[part.ordinal()];
    }

    public void setBleeding(BodyPart part, BleedingSeverity bleeding) {
        this.bleeding[part.ordinal()] = Objects.requireNonNull(bleeding, "bleeding");
        this.lastAffectedPart = part;
        if (bleeding != BleedingSeverity.NONE) {
            ensurePain(part, bleeding == BleedingSeverity.HEAVY ? 50.0F : 25.0F);
        }
    }

    /** Highest regional pain value; presentation may use this without losing regional truth. */
    public float pain() {
        float result = 0.0F;
        for (float value : this.pain) {
            result = Math.max(result, value);
        }
        return result;
    }

    public float pain(BodyPart part) {
        return this.pain[part.ordinal()];
    }

    public void setPain(BodyPart part, float pain) {
        requireFinite(pain, "pain");
        this.pain[part.ordinal()] = clamp(pain, 0.0F, MAX_PAIN);
        this.lastAffectedPart = part;
    }

    public boolean isFractured(BodyPart part) {
        return this.fractures.contains(part);
    }

    public void setFractured(BodyPart part, boolean fractured) {
        requireLimb(part, "fracture");
        if (fractured) {
            this.fractures.add(part);
            ensurePain(part, 40.0F);
        } else {
            this.fractures.remove(part);
        }
        this.lastAffectedPart = part;
    }

    public Set<BodyPart> fractures() {
        return Collections.unmodifiableSet(this.fractures);
    }

    public boolean isBlackened(BodyPart part) {
        return this.blackened.contains(part);
    }

    public void setBlackened(BodyPart part, boolean value) {
        requireLimb(part, "blackened state");
        if (value) {
            this.blackened.add(part);
            this.health[part.ordinal()] = 0.0F;
            ensurePain(part, 75.0F);
        } else {
            this.blackened.remove(part);
            this.health[part.ordinal()] = Math.max(1.0F, this.health[part.ordinal()]);
        }
        this.lastAffectedPart = part;
    }

    public Set<BodyPart> blackened() {
        return Collections.unmodifiableSet(this.blackened);
    }

    public int fracturedArmCount() {
        return count(this.fractures, true);
    }

    public int fracturedLegCount() {
        return count(this.fractures, false);
    }

    public int blackenedArmCount() {
        return count(this.blackened, true);
    }

    public int blackenedLegCount() {
        return count(this.blackened, false);
    }

    /** Tarkov vital-part rule: zero head or thorax health is lethal. */
    public boolean isLethal() {
        return health(BodyPart.HEAD) <= 0.0F || health(BodyPart.THORAX) <= 0.0F;
    }

    /** Repair permanently clears one fracture first, otherwise one blackened limb, plus one regional pain state. */
    public TreatmentResult applyRepair() {
        BodyPart treated = null;
        TreatmentResult.TreatedCondition condition = TreatmentResult.TreatedCondition.NONE;
        for (BodyPart part : BodyPart.values()) {
            if (this.fractures.remove(part)) {
                treated = part;
                condition = TreatmentResult.TreatedCondition.FRACTURE;
                break;
            }
        }
        if (treated == null) {
            for (BodyPart part : BodyPart.values()) {
                if (this.blackened.contains(part)) {
                    heal(part, 0.0F, HealingSource.REPAIR);
                    treated = part;
                    condition = TreatmentResult.TreatedCondition.BLACKENED;
                    break;
                }
            }
        }
        BodyPart painPart = treated != null && pain(treated) > 0.0F ? treated : firstPainPart();
        boolean painCleared = painPart != null;
        if (painPart != null) {
            this.pain[painPart.ordinal()] = 0.0F;
            this.lastAffectedPart = painPart;
        }
        return new TreatmentResult(condition, treated, painCleared);
    }

    /** Vanilla regeneration unlocks at most one blackened limb per effect application. */
    public TreatmentResult applyRegenerationUnlock() {
        for (BodyPart part : BodyPart.values()) {
            if (this.blackened.contains(part)) {
                heal(part, 0.0F, HealingSource.REGENERATION);
                return new TreatmentResult(TreatmentResult.TreatedCondition.BLACKENED, part, false);
            }
        }
        return new TreatmentResult(TreatmentResult.TreatedCondition.NONE, null, false);
    }

    public void clearInjuries() {
        this.fractures.clear();
        this.blackened.clear();
        Arrays.fill(this.bleeding, BleedingSeverity.NONE);
        Arrays.fill(this.pain, 0.0F);
        for (BodyPart part : BodyPart.values()) {
            this.health[part.ordinal()] = this.maximumHealth[part.ordinal()];
        }
    }

    private BodyPart firstPainPart() {
        for (BodyPart part : BodyPart.values()) {
            if (pain(part) > 0.0F) {
                return part;
            }
        }
        return null;
    }

    private float totalHealth() {
        float result = 0.0F;
        for (float value : this.health) result += value;
        return result;
    }

    private void distributeOverflow(BodyPart source, float amount) {
        float remaining = amount;
        for (int pass = 0; pass < BodyPart.values().length && remaining > 0.0001F; pass++) {
            int recipients = 0;
            for (BodyPart part : BodyPart.values()) {
                if (part != source && health(part) > 0.0F) recipients++;
            }
            if (recipients == 0) return;
            float share = remaining / recipients;
            float next = 0.0F;
            for (BodyPart part : BodyPart.values()) {
                if (part == source || health(part) <= 0.0F) continue;
                float applied = Math.min(share, health(part));
                setHealth(part, health(part) - applied);
                next += share - applied;
            }
            if (next >= remaining - 0.0001F) return;
            remaining = next;
        }
    }

    private static float overflowMultiplier(BodyPart part) {
        return switch (part) {
            case STOMACH -> 1.5F;
            case LEFT_ARM, RIGHT_ARM -> 0.7F;
            case LEFT_LEG, RIGHT_LEG -> 1.0F;
            case HEAD, THORAX -> 0.0F;
        };
    }

    private void ensurePain(BodyPart part, float minimum) {
        this.pain[part.ordinal()] = Math.max(this.pain[part.ordinal()], minimum);
    }

    private static int count(Set<BodyPart> parts, boolean arms) {
        int result = 0;
        for (BodyPart part : parts) {
            if (arms ? part.isArm() : part.isLeg()) {
                result++;
            }
        }
        return result;
    }

    private static void requireLimb(BodyPart part, String field) {
        Objects.requireNonNull(part, "part");
        if (!part.isLimb()) {
            throw new IllegalArgumentException(field + " only supports arms and legs");
        }
    }

    private static void requireFinitePositive(float value, String field) {
        requireFinite(value, field);
        if (value <= 0.0F) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireFiniteNonNegative(float value, String field) {
        requireFinite(value, field);
        if (value < 0.0F) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
    }

    private static void requireFinite(float value, String field) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InjuryState state)) return false;
        return this.lastAffectedPart == state.lastAffectedPart
                && Arrays.equals(this.health, state.health)
                && Arrays.equals(this.maximumHealth, state.maximumHealth)
                && Arrays.equals(this.bleeding, state.bleeding)
                && Arrays.equals(this.pain, state.pain)
                && this.fractures.equals(state.fractures)
                && this.blackened.equals(state.blackened);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(this.health);
        result = 31 * result + Arrays.hashCode(this.maximumHealth);
        result = 31 * result + Arrays.hashCode(this.bleeding);
        result = 31 * result + Arrays.hashCode(this.pain);
        result = 31 * result + this.fractures.hashCode();
        result = 31 * result + this.blackened.hashCode();
        result = 31 * result + this.lastAffectedPart.hashCode();
        return result;
    }
}
