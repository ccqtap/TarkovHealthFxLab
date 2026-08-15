package com.qq.tarkovhealthfxlab.client;

import com.qq.tarkovhealthfxlab.common.health.BleedingSeverity;
import com.qq.tarkovhealthfxlab.common.health.BodyPart;
import com.qq.tarkovhealthfxlab.common.health.InjuryState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LabInjuryStateMapperTest {
    @Test
    void mapsAllRegionalTruthAndDerivesAggregateHealth() {
        InjuryState truth = new InjuryState();
        truth.setMaximumHealth(BodyPart.LEFT_ARM, 80.0F);
        truth.setHealth(BodyPart.LEFT_ARM, 24.0F);
        truth.setBleeding(BodyPart.LEFT_ARM, BleedingSeverity.HEAVY);
        truth.setFractured(BodyPart.LEFT_ARM, true);
        truth.setPain(BodyPart.LEFT_ARM, 67.0F);
        truth.setBlackened(BodyPart.RIGHT_LEG, true);

        HealthFxState mapped = LabInjuryStateMapper.fromTruth(truth, true);

        BodyEffect arm = mapped.effect(BodyRegion.LEFT_ARM);
        assertEquals(BleedingLevel.HEAVY, arm.bleeding());
        assertTrue(arm.fractured());
        assertFalse(arm.blackened());
        assertEquals(67.0D, arm.pain());
        assertEquals(24.0D, arm.currentHealth());
        assertEquals(80.0D, arm.maximumHealth());
        assertTrue(mapped.effect(BodyRegion.RIGHT_LEG).blackened());
        assertEquals(1, mapped.blackenedLegs());
        assertTrue(mapped.painkillerActive());
        assertEquals(mapped.totalCurrentHealth() / mapped.totalMaximumHealth(), mapped.healthRatio(), 0.000001D);
    }
}
