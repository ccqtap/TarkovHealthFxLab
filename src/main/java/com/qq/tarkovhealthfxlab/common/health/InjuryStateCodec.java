package com.qq.tarkovhealthfxlab.common.health;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class InjuryStateCodec {
    public static final int SCHEMA_VERSION = 2;

    private InjuryStateCodec() {
    }

    public static CompoundTag encode(InjuryState state) {
        CompoundTag result = new CompoundTag();
        result.putInt("schema", SCHEMA_VERSION);
        result.putString("last_affected", state.lastAffectedPart().name());
        CompoundTag parts = new CompoundTag();
        for (BodyPart part : BodyPart.values()) {
            CompoundTag value = new CompoundTag();
            value.putFloat("health", state.health(part));
            value.putFloat("maximum", state.maximumHealth(part));
            value.putString("bleeding", state.bleeding(part).name());
            value.putFloat("pain", state.pain(part));
            value.putBoolean("fractured", state.isFractured(part));
            value.putBoolean("blackened", state.isBlackened(part));
            parts.put(part.id(), value);
        }
        result.put("parts", parts);
        return result;
    }

    public static InjuryState decode(CompoundTag tag) {
        InjuryState result = new InjuryState();
        if (tag == null || !tag.contains("parts", Tag.TAG_COMPOUND)) {
            return result;
        }
        CompoundTag parts = tag.getCompound("parts");
        for (BodyPart part : BodyPart.values()) {
            if (!parts.contains(part.id(), Tag.TAG_COMPOUND)) continue;
            CompoundTag value = parts.getCompound(part.id());
            if (value.contains("maximum", Tag.TAG_ANY_NUMERIC)) {
                float maximum = value.getFloat("maximum");
                if (Float.isFinite(maximum) && maximum > 0.0F) {
                    result.setMaximumHealth(part, maximum);
                }
            }
            if (value.contains("health", Tag.TAG_ANY_NUMERIC)) {
                float health = value.getFloat("health");
                if (Float.isFinite(health)) result.setHealth(part, health);
            }
            if (value.contains("bleeding", Tag.TAG_STRING)) {
                try {
                    result.setBleeding(part, BleedingSeverity.valueOf(value.getString("bleeding")));
                } catch (IllegalArgumentException ignored) {
                    result.setBleeding(part, BleedingSeverity.NONE);
                }
            }
            if (value.contains("pain", Tag.TAG_ANY_NUMERIC)) {
                float pain = value.getFloat("pain");
                if (Float.isFinite(pain)) result.setPain(part, pain);
            }
            if (part.isLimb()) {
                if (value.contains("fractured", Tag.TAG_BYTE) && value.getBoolean("fractured")) {
                    result.setFractured(part, true);
                }
                if (value.contains("blackened", Tag.TAG_BYTE) && value.getBoolean("blackened")) {
                    result.setBlackened(part, true);
                }
            }
        }
        if (tag.contains("last_affected", Tag.TAG_STRING)) {
            try {
                result.setLastAffectedPart(BodyPart.valueOf(tag.getString("last_affected")));
            } catch (IllegalArgumentException ignored) {
                result.setLastAffectedPart(BodyPart.STOMACH);
            }
        }
        return result;
    }
}
