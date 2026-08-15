package com.qq.tarkovhealthfxlab.compat.lrtactical;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MedicalCompatTuningTest {
    @Test
    void defaultIbuprofenDurationIsTwoMinutes() {
        assertEquals(2400, MedicalCompatTuning.DEFAULT.ibuprofenAnalgesiaDurationTicks());
    }

    @Test
    void durationCannotBeNonPositive() {
        assertEquals(1, new MedicalCompatTuning(0).ibuprofenAnalgesiaDurationTicks());
    }
}
