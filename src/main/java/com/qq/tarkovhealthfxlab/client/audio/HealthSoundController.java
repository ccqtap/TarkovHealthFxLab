package com.qq.tarkovhealthfxlab.client.audio;

import com.qq.tarkovhealthfxlab.client.BleedingLevel;
import com.qq.tarkovhealthfxlab.client.EffectChannels;
import com.qq.tarkovhealthfxlab.client.HealthFxClientConfig;
import com.qq.tarkovhealthfxlab.client.HealthFxState;
import com.qq.tarkovhealthfxlab.client.compat.ClientFxCompatCoordinator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.RegistryObject;

public final class HealthSoundController {
    private static final RandomSource RANDOM = RandomSource.create(0x5441524B4F564CL);
    private static HealthFxState previous = HealthFxState.healthy();
    private static long nextBleedPulse;
    private static long nextPainBreath;
    private static long nextFractureStep;

    private HealthSoundController() {
    }

    public static void tick(
            Minecraft minecraft,
            HealthFxState state,
            EffectChannels channels,
            boolean moving,
            boolean onGround,
            long clientTick
    ) {
        if (minecraft.player == null) {
            previous = HealthFxState.healthy();
            return;
        }
        if (!HealthFxClientConfig.AUDIO_ENABLED.get()) {
            previous = state;
            return;
        }
        float volumeScale = (float) (HealthFxClientConfig.EFFECT_VOLUME.get()
                * HealthFxClientConfig.MASTER_INTENSITY.get());
        if (volumeScale <= 0.001F) {
            previous = state;
            return;
        }

        boolean legacyHeartbeatAndPain = ClientFxCompatCoordinator.useLegacyHeartbeatAndPainAudio();
        if (legacyHeartbeatAndPain
                && !HealthFxClientConfig.DISABLE_HEARTBEAT.get()
                && state.strongestBleeding().ordinal() > previous.strongestBleeding().ordinal()) {
            play(minecraft, ModSounds.BLEED_PULSE, 0.48F * volumeScale, 0.96F);
            nextBleedPulse = clientTick + bleedInterval(state.strongestBleeding());
        }
        if (state.fractureCount() + state.blackenedCount()
                > previous.fractureCount() + previous.blackenedCount()) {
            play(minecraft, ModSounds.FRACTURE_ONSET, 0.62F * volumeScale, 0.98F);
            nextFractureStep = clientTick + 12L;
        }
        if (legacyHeartbeatAndPain
                && state.visiblePain() >= previous.visiblePain() + 8.0D) {
            play(minecraft, ModSounds.PAIN_STING,
                    InjuryAudioVolume.ringing(
                            0.42F * volumeScale,
                            HealthFxClientConfig.RINGING_VOLUME.get()),
                    1.0F);
            nextPainBreath = clientTick + 80L;
        }
        boolean relief = state.strongestBleeding().ordinal() < previous.strongestBleeding().ordinal()
                || state.fractureCount() + state.blackenedCount()
                < previous.fractureCount() + previous.blackenedCount()
                || state.visiblePain() + 15.0D < previous.visiblePain();
        if (relief) {
            play(minecraft, ModSounds.RELIEF, 0.35F * volumeScale, 1.04F);
        }

        if (legacyHeartbeatAndPain
                && !HealthFxClientConfig.DISABLE_HEARTBEAT.get()
                && channels.bleeding() > 0.01D && clientTick >= nextBleedPulse) {
            double heartbeat = Math.max(channels.critical() * 0.65D, channels.bleeding() * 0.75D);
            float volume = (float) ((0.18D + heartbeat * 0.42D) * volumeScale);
            play(minecraft, ModSounds.BLEED_PULSE, volume, 0.94F + RANDOM.nextFloat() * 0.08F);
            nextBleedPulse = clientTick + bleedInterval(state.strongestBleeding());
        }
        if (legacyHeartbeatAndPain
                && channels.pain() >= 0.25D && clientTick >= nextPainBreath) {
            float volume = (float) ((0.12D + channels.pain() * 0.24D) * volumeScale);
            play(minecraft, ModSounds.PAIN_BREATH, volume, 0.96F + RANDOM.nextFloat() * 0.06F);
            nextPainBreath = clientTick + 100L + RANDOM.nextInt(81);
        }
        if (moving && onGround && state.impairedLegs() > 0 && clientTick >= nextFractureStep) {
            play(minecraft, ModSounds.FRACTURE_STEP, 0.20F * volumeScale, 0.92F + RANDOM.nextFloat() * 0.08F);
            nextFractureStep = clientTick + 11L + RANDOM.nextInt(5);
        }
        previous = state;
    }

    public static void reset() {
        previous = HealthFxState.healthy();
        nextBleedPulse = 0L;
        nextPainBreath = 0L;
        nextFractureStep = 0L;
    }

    private static long bleedInterval(BleedingLevel level) {
        return switch (level) {
            case NONE -> 240L;
            case LIGHT -> 140L + RANDOM.nextInt(101);
            case HEAVY -> 56L + RANDOM.nextInt(29);
        };
    }

    private static void play(
            Minecraft minecraft,
            RegistryObject<SoundEvent> sound,
            float volume,
            float pitch
    ) {
        if (volume <= 0.001F || !sound.isPresent()) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound.get(), pitch, volume));
    }
}
