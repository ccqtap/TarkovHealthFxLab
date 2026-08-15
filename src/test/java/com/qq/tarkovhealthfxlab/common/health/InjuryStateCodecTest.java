package com.qq.tarkovhealthfxlab.common.health;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InjuryStateCodecTest {
    @Test
    void roundTripPreservesSevenPartTruth() {
        InjuryState state = new InjuryState();
        state.setHealth(BodyPart.LEFT_ARM, 0.0F);
        state.setBleeding(BodyPart.STOMACH, BleedingSeverity.HEAVY);
        state.setFractured(BodyPart.RIGHT_LEG, true);
        state.setPain(BodyPart.THORAX, 63.0F);

        assertEquals(state, InjuryStateCodec.decode(InjuryStateCodec.encode(state)));
    }

    @Test
    void missingAndNonFiniteFieldsFallBackWithoutCrashing() {
        CompoundTag root = new CompoundTag();
        CompoundTag parts = new CompoundTag();
        CompoundTag leftArm = new CompoundTag();
        leftArm.putFloat("health", Float.NaN);
        leftArm.putFloat("maximum", Float.POSITIVE_INFINITY);
        leftArm.putFloat("pain", Float.NaN);
        leftArm.putString("bleeding", "NOT_A_LEVEL");
        parts.put(BodyPart.LEFT_ARM.id(), leftArm);
        root.put("parts", parts);

        InjuryState decoded = InjuryStateCodec.decode(root);
        assertEquals(BodyPart.LEFT_ARM.defaultMaximum(), decoded.health(BodyPart.LEFT_ARM));
        assertEquals(BodyPart.LEFT_ARM.defaultMaximum(), decoded.maximumHealth(BodyPart.LEFT_ARM));
        assertEquals(0.0F, decoded.pain(BodyPart.LEFT_ARM));
        assertEquals(BleedingSeverity.NONE, decoded.bleeding(BodyPart.LEFT_ARM));
        assertFalse(decoded.isBlackened(BodyPart.LEFT_ARM));
    }

    @Test
    void partialTagDoesNotZeroUnspecifiedParts() {
        CompoundTag root = new CompoundTag();
        root.put("parts", new CompoundTag());

        InjuryState decoded = InjuryStateCodec.decode(root);
        for (BodyPart part : BodyPart.values()) {
            assertEquals(part.defaultMaximum(), decoded.health(part));
        }
    }
}
