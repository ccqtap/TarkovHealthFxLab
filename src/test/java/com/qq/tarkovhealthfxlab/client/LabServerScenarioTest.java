package com.qq.tarkovhealthfxlab.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LabServerScenarioTest {
    @Test
    void everySceneStartsFromClearAndCancelsAnalgesia() {
        for (HealthFxPreset preset : HealthFxPreset.values()) {
            List<String> commands = LabServerScenario.commands(preset, "Tester");
            assertEquals("clear Tester", commands.get(0));
            assertEquals("analgesia off Tester", commands.get(1));
        }
    }

    @Test
    void itemSevenSceneCoversBlackenedBleedingFracturePainAndPartHp() {
        String plan = String.join("\n",
                LabServerScenario.commands(HealthFxPreset.ITEM7_SHOWCASE, "Tester"));
        assertTrue(plan.contains("blackened Tester left_arm true"));
        assertTrue(plan.contains("blackened Tester right_leg true"));
        assertTrue(plan.contains("bleeding Tester stomach heavy"));
        assertTrue(plan.contains("fracture Tester left_arm true"));
        assertTrue(plan.contains("part_hp Tester stomach 27.0"));
        assertTrue(plan.contains("pain Tester stomach 70.0"));
    }

    @Test
    void f8UsesDedicatedServerRootInsteadOfCollidingClientRoot() {
        assertEquals("healthfx_server analgesia off Tester",
                HealthFxController.serverCommandLine("analgesia off Tester"));
        assertEquals("healthfx_server head_redirect status",
                HealthFxController.serverCommandLine("head_redirect status"));
    }
}
