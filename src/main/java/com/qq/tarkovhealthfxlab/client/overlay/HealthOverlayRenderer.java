package com.qq.tarkovhealthfxlab.client.overlay;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import com.qq.tarkovhealthfxlab.client.BleedingLevel;
import com.qq.tarkovhealthfxlab.client.BodyRegion;
import com.qq.tarkovhealthfxlab.client.HealthFxClientConfig;
import com.qq.tarkovhealthfxlab.client.HealthFxController;
import com.qq.tarkovhealthfxlab.client.HealthFxFrame;
import com.qq.tarkovhealthfxlab.client.HealthFxState;
import com.qq.tarkovhealthfxlab.client.compat.ClientFxCompatCoordinator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class HealthOverlayRenderer {
    private static final int TEXTURE_SIZE = 1254;
    private static final ResourceLocation BLOOD = texture("bleeding_border.png");
    private static final ResourceLocation FRACTURE = texture("fracture_stress.png");
    private static final ResourceLocation PAIN = texture("pain_periphery.png");
    private static final ResourceLocation STATUS_BLEEDING = statusTexture("bleeding.png");
    private static final ResourceLocation STATUS_FRACTURE = statusTexture("fracture.png");
    private static final ResourceLocation STATUS_PAIN = statusTexture("pain.png");
    private static final ResourceLocation STATUS_PAINKILLER = statusTexture("painkiller.png");
    private static final ResourceLocation STATUS_BLACKENED_LEFT_ARM =
            statusTexture("blackened_left_arm.png");
    private static final ResourceLocation STATUS_BLACKENED_RIGHT_ARM =
            statusTexture("blackened_right_arm.png");
    private static final ResourceLocation STATUS_BLACKENED_LEFT_LEG =
            statusTexture("blackened_left_leg.png");
    private static final ResourceLocation STATUS_BLACKENED_RIGHT_LEG =
            statusTexture("blackened_right_leg.png");
    private static final BodyRegion[] LIMBS = {
            BodyRegion.LEFT_ARM,
            BodyRegion.RIGHT_ARM,
            BodyRegion.LEFT_LEG,
            BodyRegion.RIGHT_LEG
    };
    private static final int CARD_HEIGHT = 18;
    private static final int CARD_STEP = 20;

    private HealthOverlayRenderer() {
    }

    public static void render(
            GuiGraphics graphics,
            float partialTick,
            int screenWidth,
            int screenHeight,
            boolean includeStatusCards
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        // The global HUD overlay is not drawn over inventories or other screens.
        // The lab screen passes includeStatusCards=false and calls this method
        // explicitly so it can preview the effect without double compositing.
        if (includeStatusCards && minecraft.screen != null) {
            return;
        }
        HealthFxFrame frame = HealthFxController.frame(partialTick);
        if (HealthFxClientConfig.SCREEN_EFFECTS.get() != HealthFxClientConfig.ScreenEffectsMode.OFF
                && ClientFxCompatCoordinator.useLegacyScreenEffects()) {
            renderScreenEffects(graphics, frame, screenWidth, screenHeight);
        }
        if (includeStatusCards
                && HealthFxClientConfig.KEEP_STATUS_ICONS.get()
                && !minecraft.options.hideGui) {
            renderStatusCards(graphics, HealthFxController.displayedState(), screenWidth, screenHeight);
        }
    }

    private static void renderScreenEffects(
            GuiGraphics graphics,
            HealthFxFrame frame,
            int width,
            int height
    ) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 900.0F);
        if (frame.vignetteAlpha() > 0.001D) {
            renderVignette(graphics, width, height, frame.vignetteAlpha());
        }
        drawTexture(graphics, BLOOD, width, height, 1.0F, 0.30F, 0.28F, frame.bloodAlpha(), 0, 0);
        drawTexture(graphics, FRACTURE, width, height, 0.92F, 0.96F, 1.0F, frame.fractureAlpha(), 0, 0);
        if (frame.painAlpha() > 0.001D && HealthFxClientConfig.CHROMATIC_OFFSET.get()) {
            drawTexture(graphics, PAIN, width, height, 1.0F, 0.34F, 0.32F,
                    frame.painAlpha() * 0.25D, -1, 0);
            drawTexture(graphics, PAIN, width, height, 0.30F, 0.46F, 1.0F,
                    frame.painAlpha() * 0.25D, 1, 0);
        }
        drawTexture(graphics, PAIN, width, height, 0.88F, 0.84F, 1.0F,
                frame.painAlpha() * 0.72D, 0, 0);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().popPose();
    }

    private static void drawTexture(
            GuiGraphics graphics,
            ResourceLocation texture,
            int width,
            int height,
            float red,
            float green,
            float blue,
            double alpha,
            int offsetX,
            int offsetY
    ) {
        if (alpha <= 0.001D) {
            return;
        }
        graphics.setColor(red, green, blue, (float) Math.min(1.0D, alpha));
        graphics.blit(
                texture,
                offsetX,
                offsetY,
                width,
                height,
                0.0F,
                0.0F,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
    }

    private static void renderVignette(GuiGraphics graphics, int width, int height, double alpha) {
        int edgeX = Math.max(12, width / 12);
        int edgeY = Math.max(12, height / 10);
        int color = ((int) Math.round(Math.min(0.24D, alpha) * 255.0D) << 24) | 0x080408;
        graphics.fill(0, 0, width, edgeY, color);
        graphics.fill(0, height - edgeY, width, height, color);
        graphics.fill(0, edgeY, edgeX, height - edgeY, color);
        graphics.fill(width - edgeX, edgeY, width, height - edgeY, color);
    }

    private static void renderStatusCards(
            GuiGraphics graphics,
            HealthFxState state,
            int screenWidth,
            int screenHeight
    ) {
        int rows = 0;
        if (state.strongestBleeding() != BleedingLevel.NONE) rows++;
        for (BodyRegion limb : LIMBS) {
            if (state.effect(limb).blackened()) rows++;
        }
        if (state.fractureCount() > 0) rows++;
        if (state.rawPain() > 0.0D) rows++;
        if (state.painkillerActive()) rows++;
        if (rows == 0) return;

        Font font = Minecraft.getInstance().font;
        boolean highContrast = HealthFxClientConfig.HIGH_CONTRAST.get();
        int x = 8;
        int y = Math.max(8, screenHeight - 74 - rows * CARD_STEP);
        if (state.strongestBleeding() != BleedingLevel.NONE) {
            Component text = Component.translatable(
                    state.strongestBleeding() == BleedingLevel.HEAVY
                            ? "healthfx.status.heavy_bleed"
                            : "healthfx.status.light_bleed");
            drawCard(graphics, font, x, y,
                    highContrast ? 0xFFFFFFFF : 0xFFE15A4F, text, STATUS_BLEEDING);
            y += CARD_STEP;
        }
        for (BodyRegion limb : LIMBS) {
            if (!state.effect(limb).blackened()) continue;
            drawCard(graphics, font, x, y, highContrast ? 0xFFFFFFFF : 0xFFC3B6B6,
                    Component.translatable("healthfx.status.blackened_" + limb.id()),
                    blackenedTexture(limb));
            y += CARD_STEP;
        }
        if (state.fractureCount() > 0) {
            drawCard(graphics, font, x, y, highContrast ? 0xFFFFFFFF : 0xFFE5E8EA,
                    Component.translatable("healthfx.status.fracture", state.fractureCount()),
                    STATUS_FRACTURE);
            y += CARD_STEP;
        }
        if (state.rawPain() > 0.0D) {
            int color = highContrast
                    ? 0xFFFFFF66
                    : (state.visiblePain() >= 60.0D ? 0xFFFFB84D : 0xFFE3C076);
            drawCard(graphics, font, x, y, color,
                    Component.translatable("healthfx.status.pain", Math.round(state.rawPain())),
                    STATUS_PAIN);
            y += CARD_STEP;
        }
        if (state.painkillerActive()) {
            drawCard(graphics, font, x, y, highContrast ? 0xFF7FFFFF : 0xFF65D8D1,
                    Component.translatable("healthfx.status.painkiller"),
                    STATUS_PAINKILLER);
        }
    }

    private static void drawCard(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int color,
            Component text,
            ResourceLocation icon
    ) {
        int width = Math.max(100, font.width(text) + 34);
        graphics.fill(x, y, x + width, y + CARD_HEIGHT, 0xB4090B0E);
        graphics.fill(x, y, x + 2, y + CARD_HEIGHT, color);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(icon, x + 4, y, 18, 18, 0.0F, 0.0F, 18, 18, 18, 18);
        graphics.drawString(font, text, x + 26, y + 5, color, true);
    }

    private static ResourceLocation blackenedTexture(BodyRegion limb) {
        return switch (limb) {
            case LEFT_ARM -> STATUS_BLACKENED_LEFT_ARM;
            case RIGHT_ARM -> STATUS_BLACKENED_RIGHT_ARM;
            case LEFT_LEG -> STATUS_BLACKENED_LEFT_LEG;
            case RIGHT_LEG -> STATUS_BLACKENED_RIGHT_LEG;
            default -> throw new IllegalArgumentException("Not a limb: " + limb);
        };
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(
                TarkovHealthFxLab.MODID,
                "textures/gui/health_fx/" + name
        );
    }

    private static ResourceLocation statusTexture(String name) {
        return new ResourceLocation(
                TarkovHealthFxLab.MODID,
                "textures/gui/status/" + name
        );
    }
}
