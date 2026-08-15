package com.qq.tarkovhealthfxlab.common.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InjuryStateTest {
    @Test
    void sevenPartsHaveIndependentHealthBleedingAndPain() {
        InjuryState state = new InjuryState();
        state.setBleeding(BodyPart.LEFT_ARM, BleedingSeverity.HEAVY);

        assertEquals(BleedingSeverity.HEAVY, state.bleeding(BodyPart.LEFT_ARM));
        assertEquals(BleedingSeverity.NONE, state.bleeding(BodyPart.RIGHT_ARM));
        assertEquals(50.0F, state.pain(BodyPart.LEFT_ARM));
        assertEquals(0.0F, state.pain(BodyPart.RIGHT_ARM));
        assertEquals(BodyPart.LEFT_ARM, state.lastAffectedPart());
    }

    @Test
    void zeroHealthAutomaticallyBlackensLimbAndOrdinaryHealingIsLocked() {
        InjuryState state = new InjuryState();
        state.damage(BodyPart.LEFT_LEG, state.maximumHealth(BodyPart.LEFT_LEG));

        assertTrue(state.isBlackened(BodyPart.LEFT_LEG));
        assertEquals(0.0F, state.heal(BodyPart.LEFT_LEG, 30.0F, HealingSource.ORDINARY));
        assertEquals(0.0F, state.health(BodyPart.LEFT_LEG));

        assertTrue(state.heal(BodyPart.LEFT_LEG, 10.0F, HealingSource.REGENERATION) > 0.0F);
        assertFalse(state.isBlackened(BodyPart.LEFT_LEG));
        assertTrue(state.health(BodyPart.LEFT_LEG) > 0.0F);
    }

    @Test
    void repairTreatsOneFractureBeforeOneBlackenedLimbAndOnePainRegion() {
        InjuryState state = new InjuryState();
        state.setFractured(BodyPart.RIGHT_ARM, true);
        state.setBlackened(BodyPart.LEFT_LEG, true);

        TreatmentResult first = state.applyRepair();
        assertEquals(TreatmentResult.TreatedCondition.FRACTURE, first.condition());
        assertEquals(BodyPart.RIGHT_ARM, first.part());
        assertFalse(state.isFractured(BodyPart.RIGHT_ARM));
        assertTrue(state.isBlackened(BodyPart.LEFT_LEG));
        assertEquals(0.0F, state.pain(BodyPart.RIGHT_ARM));

        TreatmentResult second = state.applyRepair();
        assertEquals(TreatmentResult.TreatedCondition.BLACKENED, second.condition());
        assertEquals(BodyPart.LEFT_LEG, second.part());
        assertFalse(state.isBlackened(BodyPart.LEFT_LEG));
        assertTrue(state.health(BodyPart.LEFT_LEG) >= 1.0F);
    }

    @Test
    void everyNegativeWriteCreatesRegionalPain() {
        InjuryState state = new InjuryState();
        state.setFractured(BodyPart.LEFT_ARM, true);
        state.setBlackened(BodyPart.RIGHT_LEG, true);
        state.setBleeding(BodyPart.STOMACH, BleedingSeverity.LIGHT);

        assertTrue(state.pain(BodyPart.LEFT_ARM) > 0.0F);
        assertTrue(state.pain(BodyPart.RIGHT_LEG) > 0.0F);
        assertTrue(state.pain(BodyPart.STOMACH) > 0.0F);
    }

    @Test
    void zeroHeadOrThoraxIsLethalButAZeroLimbIsNot() {
        InjuryState limb = new InjuryState();
        limb.setHealth(BodyPart.LEFT_ARM, 0.0F);
        assertFalse(limb.isLethal());

        InjuryState head = new InjuryState();
        head.setHealth(BodyPart.HEAD, 0.0F);
        assertTrue(head.isLethal());

        InjuryState thorax = new InjuryState();
        thorax.setHealth(BodyPart.THORAX, 0.0F);
        assertTrue(thorax.isLethal());
    }

    @Test
    void damageToABlackenedLimbOverflowsAcrossLivingParts() {
        InjuryState state = new InjuryState();
        state.setHealth(BodyPart.LEFT_ARM, 0.0F);
        float beforeThorax = state.health(BodyPart.THORAX);

        float applied = state.damage(BodyPart.LEFT_ARM, 10.0F);

        assertEquals(7.0F, applied, 0.01F);
        assertTrue(state.health(BodyPart.THORAX) < beforeThorax);
        assertEquals(BodyPart.LEFT_ARM, state.lastAffectedPart());
    }

    @Test
    void regenerationUnlockRestoresOnlyOneBlackenedLimbPerApplication() {
        InjuryState state = new InjuryState();
        state.setBlackened(BodyPart.LEFT_ARM, true);
        state.setBlackened(BodyPart.RIGHT_ARM, true);

        TreatmentResult first = state.applyRegenerationUnlock();
        assertEquals(TreatmentResult.TreatedCondition.BLACKENED, first.condition());
        assertFalse(state.isBlackened(first.part()));
        assertEquals(1, state.blackenedArmCount());

        // The effect bridge marks the active regeneration instance as used;
        // vanilla hearts are subsequently re-projected every server tick.
        assertTrue(state.health(first.part()) >= 1.0F);
    }
}
