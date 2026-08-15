package com.qq.tarkovhealthfxlab.common.effect;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TarkovHealthFxLab.MODID);

    public static final RegistryObject<MobEffect> LIGHT_BLEEDING = injury("light_bleeding", 0xA94442);
    public static final RegistryObject<MobEffect> HEAVY_BLEEDING = injury("heavy_bleeding", 0x6F1515);
    public static final RegistryObject<MobEffect> PAIN = injury("pain", 0x50404A);
    public static final RegistryObject<MobEffect> FRACTURE = injury("fracture", 0xD2C7B4);
    public static final RegistryObject<MobEffect> BLACKENED_LEFT_ARM = injury("blackened_left_arm", 0x292527);
    public static final RegistryObject<MobEffect> BLACKENED_RIGHT_ARM = injury("blackened_right_arm", 0x292527);
    public static final RegistryObject<MobEffect> BLACKENED_LEFT_LEG = injury("blackened_left_leg", 0x201E20);
    public static final RegistryObject<MobEffect> BLACKENED_RIGHT_LEG = injury("blackened_right_leg", 0x201E20);
    public static final RegistryObject<MobEffect> REPAIR = beneficial("repair", 0x8FC7A0);
    public static final RegistryObject<MobEffect> ANALGESIA = beneficial("analgesia", 0x78A9C7);

    private ModEffects() {
    }

    private static RegistryObject<MobEffect> injury(String id, int color) {
        return register(id, MobEffectCategory.NEUTRAL, color);
    }

    private static RegistryObject<MobEffect> beneficial(String id, int color) {
        return register(id, MobEffectCategory.BENEFICIAL, color);
    }

    private static RegistryObject<MobEffect> register(
            String id,
            MobEffectCategory category,
            int color
    ) {
        return MOB_EFFECTS.register(id, () -> new LabMobEffect(category, color));
    }
}
