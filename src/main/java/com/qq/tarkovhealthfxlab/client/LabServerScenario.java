package com.qq.tarkovhealthfxlab.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure command plan for applying an F8 scene to the authoritative laboratory source. */
public final class LabServerScenario {
    private LabServerScenario() {
    }

    public static List<String> commands(HealthFxPreset preset, String target) {
        Objects.requireNonNull(preset, "preset");
        String safeTarget = Objects.requireNonNull(target, "target");
        List<String> commands = new ArrayList<>();
        commands.add("clear " + safeTarget);
        commands.add("analgesia off " + safeTarget);
        switch (preset) {
            case OFF -> {
            }
            case LIGHT_BLEED -> {
                commands.add("set part_hp " + safeTarget + " left_arm 49.0");
                commands.add("set bleeding " + safeTarget + " left_arm light");
            }
            case HEAVY_BLEED -> {
                commands.add("set part_hp " + safeTarget + " stomach 30.0");
                commands.add("set bleeding " + safeTarget + " stomach heavy");
            }
            case FRACTURE_LEG -> commands.add("set fracture " + safeTarget + " left_leg true");
            case BLACKENED_ARM -> commands.add("set blackened " + safeTarget + " left_arm true");
            case BLACKENED_LEG -> commands.add("set blackened " + safeTarget + " right_leg true");
            case PAIN_HIGH -> commands.add("set pain " + safeTarget + " thorax 82.0");
            case CRITICAL_MIXED -> {
                commands.add("set blackened " + safeTarget + " left_leg true");
                commands.add("set bleeding " + safeTarget + " left_leg heavy");
                commands.add("set fracture " + safeTarget + " left_leg true");
                commands.add("set part_hp " + safeTarget + " right_arm 13.0");
                commands.add("set bleeding " + safeTarget + " right_arm light");
                commands.add("set fracture " + safeTarget + " right_arm true");
                commands.add("set pain " + safeTarget + " left_leg 88.0");
            }
            case ITEM7_SHOWCASE -> {
                commands.add("set blackened " + safeTarget + " left_arm true");
                commands.add("set bleeding " + safeTarget + " left_arm heavy");
                commands.add("set fracture " + safeTarget + " left_arm true");
                commands.add("set blackened " + safeTarget + " right_leg true");
                commands.add("set bleeding " + safeTarget + " right_leg light");
                commands.add("set fracture " + safeTarget + " right_leg true");
                commands.add("set part_hp " + safeTarget + " stomach 27.0");
                commands.add("set bleeding " + safeTarget + " stomach heavy");
                commands.add("set pain " + safeTarget + " stomach 70.0");
            }
        }
        return List.copyOf(commands);
    }
}
