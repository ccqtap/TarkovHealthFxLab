package com.qq.tarkovhealthfxlab.client;

import net.minecraftforge.common.ForgeConfigSpec;

public final class HealthFxClientConfig {
    public enum ScreenEffectsMode {
        OFF,
        LOW,
        FULL;

        public ScreenEffectsMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.EnumValue<ScreenEffectsMode> SCREEN_EFFECTS = BUILDER
            .comment("Full-screen injury textures. OFF keeps status cards available.")
            .defineEnum("screenEffects", ScreenEffectsMode.FULL);
    public static final ForgeConfigSpec.BooleanValue KEEP_STATUS_ICONS = BUILDER
            .comment("Keep non-flashing text and shape status cards visible.")
            .define("keepStatusIcons", true);
    public static final ForgeConfigSpec.BooleanValue REDUCE_MOTION = BUILDER
            .comment("Disable injury-driven camera motion and soften pulsing.")
            .define("reduceMotion", false);
    public static final ForgeConfigSpec.BooleanValue CHROMATIC_OFFSET = BUILDER
            .comment("Allow the pain texture's subtle one-pixel color separation.")
            .define("chromaticOffset", true);
    public static final ForgeConfigSpec.BooleanValue DISABLE_HEARTBEAT = BUILDER
            .comment("Disable recurring bleeding pulse audio while retaining other injury cues.")
            .define("disableHeartbeat", false);
    public static final ForgeConfigSpec.BooleanValue HIGH_CONTRAST = BUILDER
            .comment("Use high-contrast status card colors in addition to their shapes and labels.")
            .define("highContrast", false);
    public static final ForgeConfigSpec.BooleanValue AUDIO_ENABLED = BUILDER
            .comment("Enable original injury audio cues. Minecraft master/player volume still applies.")
            .define("audioEnabled", true);
    public static final ForgeConfigSpec.DoubleValue MASTER_INTENSITY = BUILDER
            .comment("Overall visual and audio intensity, from 0 to 1.")
            .defineInRange("masterIntensity", 0.80D, 0.0D, 1.0D);
    public static final ForgeConfigSpec.DoubleValue CAMERA_INTENSITY = BUILDER
            .comment("First-person camera feedback, from 0 to 1.")
            .defineInRange("cameraIntensity", 0.35D, 0.0D, 1.0D);
    public static final ForgeConfigSpec.DoubleValue EFFECT_VOLUME = BUILDER
            .comment("Relative injury sound volume, from 0 to 1.")
            .defineInRange("effectVolume", 0.65D, 0.0D, 1.0D);
    public static final ForgeConfigSpec.DoubleValue RINGING_VOLUME = BUILDER
            .comment("Relative pain tinnitus volume, from 0 to 1. Multiplies the general injury sound volume.")
            .defineInRange("ringingVolume", 1.0D, 0.0D, 1.0D);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private HealthFxClientConfig() {
    }
}
