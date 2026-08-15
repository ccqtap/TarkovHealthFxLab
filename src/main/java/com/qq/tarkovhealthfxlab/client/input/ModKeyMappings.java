package com.qq.tarkovhealthfxlab.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final KeyMapping OPEN_LAB = new KeyMapping(
            "key.tarkov_health_fx_lab.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "key.categories.tarkov_health_fx_lab"
    );

    private ModKeyMappings() {
    }
}
