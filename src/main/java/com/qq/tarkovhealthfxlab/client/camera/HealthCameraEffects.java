package com.qq.tarkovhealthfxlab.client.camera;

import com.qq.tarkovhealthfxlab.client.HealthFxClientConfig;
import com.qq.tarkovhealthfxlab.client.HealthFxController;
import com.qq.tarkovhealthfxlab.client.HealthFxFrame;
import com.qq.tarkovhealthfxlab.client.compat.ClientFxCompatCoordinator;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ViewportEvent;

public final class HealthCameraEffects {
    private HealthCameraEffects() {
    }

    public static void apply(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientFxCompatCoordinator.useForgeCameraFallback()
                || HealthFxClientConfig.SCREEN_EFFECTS.get() == HealthFxClientConfig.ScreenEffectsMode.OFF
                || HealthFxClientConfig.REDUCE_MOTION.get()
                || minecraft.player == null
                || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        HealthFxFrame frame = HealthFxController.frame((float) event.getPartialTick());
        event.setPitch(event.getPitch() + (float) frame.cameraPitch());
        event.setRoll(event.getRoll() + (float) frame.cameraRoll());
    }
}
