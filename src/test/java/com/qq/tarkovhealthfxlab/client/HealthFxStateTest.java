package com.qq.tarkovhealthfxlab.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HealthFxStateTest {
    @Test
    void aggregatesStrongestBleedingFracturesAndVisiblePain() {
        HealthFxState state = HealthFxState.restore(Map.of(
                BodyRegion.LEFT_ARM, new BodyEffect(BleedingLevel.LIGHT, true, 35.0D),
                BodyRegion.RIGHT_LEG, new BodyEffect(BleedingLevel.HEAVY, true, 78.0D)
        ), 0.42D, false, 7L);

        assertEquals(BleedingLevel.HEAVY, state.strongestBleeding());
        assertEquals(2, state.bleedingPartCount());
        assertEquals(2, state.fractureCount());
        assertEquals(1, state.fracturedArms());
        assertEquals(1, state.fracturedLegs());
        assertEquals(78.0D, state.visiblePain());
    }

    @Test
    void painkillerSuppressesPresentationWithoutDeletingRawPain() {
        HealthFxState state = HealthFxState.restore(Map.of(
                BodyRegion.THORAX, new BodyEffect(BleedingLevel.NONE, false, 82.0D)
        ), 0.8D, true, 2L);

        assertEquals(82.0D, state.rawPain());
        assertEquals(0.0D, state.visiblePain());
        assertEquals(0.0D, EffectChannels.from(state).pain());
    }

    @Test
    void plainHealthMutationDoesNotSecretlyChangeBlackenedTruth() {
        BodyEffect healthyAtZero = BodyEffect.healthy(60.0D).withCurrentHealth(0.0D);
        assertFalse(healthyAtZero.blackened());

        BodyEffect blackenedHealedNumerically = new BodyEffect(
                BleedingLevel.NONE, false, true, 75.0D, 0.0D, 60.0D)
                .withCurrentHealth(30.0D);
        assertTrue(blackenedHealedNumerically.blackened());
        assertEquals(30.0D, blackenedHealedNumerically.currentHealth());
    }

    @Test
    void mockZeroHpExplicitlyBlackensOnlyLimbs() {
        BodyEffect arm = MockInjuryMutations.setPartHealthRatio(
                BodyRegion.LEFT_ARM, BodyEffect.healthy(60.0D), 0.0D);
        BodyEffect thorax = MockInjuryMutations.setPartHealthRatio(
                BodyRegion.THORAX, BodyEffect.healthy(85.0D), 0.0D);

        assertTrue(arm.blackened());
        assertFalse(thorax.blackened());
    }
}
