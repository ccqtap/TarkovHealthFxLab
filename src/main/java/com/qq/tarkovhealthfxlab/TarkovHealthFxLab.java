package com.qq.tarkovhealthfxlab;

import com.qq.tarkovhealthfxlab.client.HealthFxClientConfig;
import com.qq.tarkovhealthfxlab.client.audio.ModSounds;
import com.qq.tarkovhealthfxlab.common.effect.ModEffects;
import com.qq.tarkovhealthfxlab.network.HealthFxNetwork;
import com.qq.tarkovhealthfxlab.compat.HealthCompatBindings;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TarkovHealthFxLab.MODID)
public final class TarkovHealthFxLab {
    public static final String MODID = "tarkov_health_fx_lab";

    public TarkovHealthFxLab() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.SOUND_EVENTS.register(modBus);
        ModEffects.MOB_EFFECTS.register(modBus);
        HealthFxNetwork.initialize();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HealthFxClientConfig.SPEC);
        HealthCompatBindings.install();
    }
}
