package com.qq.tarkovhealthfxlab.compat;

import com.qq.tarkovhealthfxlab.common.health.BodyPart;
import com.qq.tarkovhealthfxlab.common.health.InjuryState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthCompatBindingsTest {
    @Test
    void blackenedArmDoesNotAlsoCountAsFractured() {
        InjuryState state = new InjuryState();
        state.setFractured(BodyPart.LEFT_ARM, true);
        state.setBlackened(BodyPart.LEFT_ARM, true);
        state.setFractured(BodyPart.RIGHT_ARM, true);

        var result = HealthCompatBindings.armState(state, true);

        assertEquals(1, result.fracturedArms());
        assertEquals(1, result.blackenedArms());
        assertTrue(result.analgesiaActive());
    }
}
