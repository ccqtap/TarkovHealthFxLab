package com.qq.tarkovhealthfxlab.compat.lrtactical;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LrConsumablePolicyTest {
    @Test
    void bloodPackRequestsRepairAtLrCompletion() {
        assertEquals(
                MedicalActionSink.MedicalAction.REPAIR,
                LrConsumablePolicy.actionFor("lrtactical:blood_pack").orElseThrow()
        );
    }

    @Test
    void ibuprofenRequestsAnalgesiaAtLrCompletion() {
        assertEquals(
                MedicalActionSink.MedicalAction.ANALGESIA,
                LrConsumablePolicy.actionFor("lrtactical:ibuprofen").orElseThrow()
        );
    }

    @Test
    void unrelatedConsumablesDoNotTriggerMedicalRules() {
        assertTrue(LrConsumablePolicy.actionFor("lrtactical:condensed_milk").isEmpty());
        assertTrue(LrConsumablePolicy.actionFor("minecraft:apple").isEmpty());
        assertTrue(LrConsumablePolicy.actionFor(null).isEmpty());
    }
}
