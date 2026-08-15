package com.qq.tarkovhealthfxlab.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialEffectTrackerTest {
    @Test
    void usedTreatmentRemainsConsumedForTheWholeActiveInstance() {
        int marker = SpecialEffectTracker.carryActive(0, true, true, false);
        marker = SpecialEffectTracker.markRepairUsed(marker);
        marker = SpecialEffectTracker.markRegenerationUsed(marker);

        int nextTick = SpecialEffectTracker.carryActive(marker, true, true, false);
        assertTrue(SpecialEffectTracker.repairUsed(nextTick));
        assertTrue(SpecialEffectTracker.regenerationUsed(nextTick));
    }

    @Test
    void endingAnEffectRearmsItsNextInstance() {
        int marker = SpecialEffectTracker.markRepairUsed(
                SpecialEffectTracker.markRegenerationUsed(0));
        int inactive = SpecialEffectTracker.carryActive(marker, false, false, false);
        int nextInstance = SpecialEffectTracker.carryActive(inactive, true, true, false);

        assertFalse(SpecialEffectTracker.repairUsed(nextInstance));
        assertFalse(SpecialEffectTracker.regenerationUsed(nextInstance));
    }

    @Test
    void refreshingAnActiveInstanceRearmsExactlyThatTreatment() {
        int marker = SpecialEffectTracker.markRepairUsed(
                SpecialEffectTracker.markRegenerationUsed(0));

        int repairRefresh = SpecialEffectTracker.rearmRepair(marker);
        assertFalse(SpecialEffectTracker.repairUsed(repairRefresh));
        assertTrue(SpecialEffectTracker.regenerationUsed(repairRefresh));

        int regenerationRefresh = SpecialEffectTracker.rearmRegeneration(marker);
        assertTrue(SpecialEffectTracker.repairUsed(regenerationRefresh));
        assertFalse(SpecialEffectTracker.regenerationUsed(regenerationRefresh));
    }
}
