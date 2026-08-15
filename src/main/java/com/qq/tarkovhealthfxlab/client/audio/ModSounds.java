package com.qq.tarkovhealthfxlab.client.audio;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TarkovHealthFxLab.MODID);

    public static final RegistryObject<SoundEvent> BLEED_PULSE = register("health_fx.bleed_pulse");
    public static final RegistryObject<SoundEvent> FRACTURE_ONSET = register("health_fx.fracture_onset");
    public static final RegistryObject<SoundEvent> FRACTURE_STEP = register("health_fx.fracture_step");
    public static final RegistryObject<SoundEvent> PAIN_STING = register("health_fx.pain_sting");
    public static final RegistryObject<SoundEvent> PAIN_BREATH = register("health_fx.pain_breath");
    public static final RegistryObject<SoundEvent> RELIEF = register("health_fx.relief");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String path) {
        ResourceLocation id = new ResourceLocation(TarkovHealthFxLab.MODID, path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
