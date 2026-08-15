package com.qq.tarkovhealthfxlab.compat.tacz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmRecoilTuningTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void healthyArmsKeepIdentityRecoil() {
        var result = ArmRecoilTuning.DEFAULT.multipliers(InjuryCompatState.ArmInjuryState.HEALTHY);

        assertEquals(1.0D, result.horizontal(), EPSILON);
        assertEquals(1.0D, result.vertical(), EPSILON);
    }

    @Test
    void fractureAndBlackenedArmUseConfirmedIndependentExcess() {
        var state = new InjuryCompatState.ArmInjuryState(1, 1, false);

        var result = ArmRecoilTuning.DEFAULT.multipliers(state);

        assertEquals(1.45D, result.horizontal(), EPSILON);
        assertEquals(1.70D, result.vertical(), EPSILON);
    }

    @Test
    void analgesiaHalvesOnlyTheInjuryExcess() {
        var state = new InjuryCompatState.ArmInjuryState(1, 1, true);

        var result = ArmRecoilTuning.DEFAULT.multipliers(state);

        assertEquals(1.225D, result.horizontal(), EPSILON);
        assertEquals(1.35D, result.vertical(), EPSILON);
    }

    @Test
    void blackenedArmsWinWhenCountsWouldOverlap() {
        var state = new InjuryCompatState.ArmInjuryState(2, 1, false);

        assertEquals(1, state.fracturedArms());
        assertEquals(1, state.blackenedArms());
    }
}
