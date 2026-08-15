package com.qq.tarkovhealthfxlab.client;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import com.qq.tarkovhealthfxlab.client.input.ModKeyMappings;
import com.qq.tarkovhealthfxlab.client.overlay.HealthOverlayRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TarkovHealthFxLab.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModBusEvents {
    private ClientModBusEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.OPEN_LAB);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("injury_effects", (gui, graphics, partialTick, width, height) ->
                HealthOverlayRenderer.render(graphics, partialTick, width, height, true));
    }
}
