package com.qq.tarkovhealthfxlab.client;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import com.qq.tarkovhealthfxlab.client.camera.HealthCameraEffects;
import com.qq.tarkovhealthfxlab.client.command.HealthFxClientCommands;
import com.qq.tarkovhealthfxlab.client.input.ModKeyMappings;
import com.qq.tarkovhealthfxlab.client.screen.HealthFxLabScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TarkovHealthFxLab.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (ModKeyMappings.OPEN_LAB.consumeClick()) {
            minecraft.setScreen(new HealthFxLabScreen());
        }
        HealthFxController.tick(minecraft);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        HealthCameraEffects.apply(event);
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        HealthFxClientCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onSystemChat(ClientChatReceivedEvent.System event) {
        if (!event.isOverlay()) {
            HealthFxController.acceptServerFeedback(event.getMessage().getString());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HealthFxController.resetSession();
    }
}
