package com.qq.tarkovhealthfxlab.common.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovementPenaltyCalculatorTest {
    @Test
    void analgesiaWeakensOnlyFractureContribution() {
        double raw = MovementPenaltyCalculator.modifierAmount(2, 1, false);
        double analgesic = MovementPenaltyCalculator.modifierAmount(2, 1, true);

        assertTrue(analgesic > raw);
        assertEquals(-0.25D, MovementPenaltyCalculator.modifierAmount(0, 1, true), 0.000001D);
    }

    @Test
    void combinedPenaltyIsCapped() {
        assertEquals(-MovementPenaltyCalculator.MAXIMUM_PENALTY,
                MovementPenaltyCalculator.modifierAmount(10, 10, false), 0.000001D);
    }
}
