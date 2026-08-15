package com.qq.tarkovhealthfxlab.compat.enhancedvisuals;

import com.mojang.logging.LogUtils;
import com.qq.tarkovhealthfxlab.client.EffectChannels;
import com.qq.tarkovhealthfxlab.client.HealthFxClientConfig;
import com.qq.tarkovhealthfxlab.client.HealthFxFrame;
import com.qq.tarkovhealthfxlab.client.HealthFxState;
import com.qq.tarkovhealthfxlab.client.audio.InjuryAudioVolume;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reflection-only adapter for EnhancedVisuals 1.8.x.
 *
 * <p>The adapter asks EnhancedVisuals to render its own built-in assets. No
 * textures, sounds, or implementation classes are bundled in this project.</p>
 */
public final class EnhancedVisualsBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SLOT_DAMAGED = "damaged";
    private static final String SLOT_LOW_HEALTH = "lowhealth";
    private static final String SLOT_TUNNEL = "tunnel";
    private static final String SLOT_CRACK = "crack";

    private final Map<String, VisualSlot> slots = new LinkedHashMap<>();
    private Method visualAdd;
    private Method visualRemove;
    private Method setOpacity;
    private Method playSound;
    private Method shouldRender;
    private Constructor<?> endlessVisualConstructor;
    private Field visualVariant;
    private Object damageHandler;
    private Object heartbeatHandler;
    private boolean active;
    private boolean screenChannelAvailable;
    private boolean failureLogged;
    private double previousPain;
    private long nextHeartbeatTick;
    private long heartbeatInTick = -1L;

    public boolean initialize(Minecraft minecraft) {
        if (active) {
            return true;
        }
        try {
            ClassLoader loader = EnhancedVisualsBridge.class.getClassLoader();
            Class<?> visualManagerClass = Class.forName(
                    "team.creative.enhancedvisuals.client.VisualManager", false, loader);
            Class<?> visualClass = Class.forName(
                    "team.creative.enhancedvisuals.api.Visual", false, loader);
            Class<?> visualTypeClass = Class.forName(
                    "team.creative.enhancedvisuals.api.type.VisualType", false, loader);
            Class<?> visualHandlerClass = Class.forName(
                    "team.creative.enhancedvisuals.api.VisualHandler", false, loader);
            Class<?> handlersClass = Class.forName(
                    "team.creative.enhancedvisuals.common.handler.VisualHandlers", false, loader);
            Class<?> clientClass = Class.forName(
                    "team.creative.enhancedvisuals.client.EVClient", false, loader);

            damageHandler = publicStaticField(handlersClass, "DAMAGE");
            heartbeatHandler = publicStaticField(handlersClass, "HEARTBEAT");
            if (damageHandler == null || heartbeatHandler == null) {
                throw new IllegalStateException("EnhancedVisuals handlers have not initialized");
            }

            Object damaged = publicField(damageHandler, "damaged");
            Object tunnel = publicField(damageHandler, "tunnel");
            Object lowHealth = publicField(heartbeatHandler, "lowhealth");
            Object crack = createCrackType(loader, visualTypeClass, minecraft);

            visualAdd = visualManagerClass.getMethod("add", visualClass);
            visualRemove = visualManagerClass.getMethod("remove", visualClass);
            setOpacity = visualClass.getMethod("setOpacityInternal", float.class);
            visualVariant = visualClass.getField("variant");
            endlessVisualConstructor = visualClass.getConstructor(
                    visualTypeClass, visualHandlerClass, int.class);
            playSound = visualHandlerClass.getMethod(
                    "playSound", ResourceLocation.class, float.class);
            shouldRender = clientClass.getMethod("shouldRender");

            slots.put(SLOT_DAMAGED, new VisualSlot(damaged, damageHandler));
            slots.put(SLOT_LOW_HEALTH, new VisualSlot(lowHealth, heartbeatHandler));
            slots.put(SLOT_TUNNEL, new VisualSlot(tunnel, damageHandler));
            slots.put(SLOT_CRACK, new VisualSlot(crack, damageHandler));
            active = true;
            LOGGER.info("Tarkov Health FX: EnhancedVisuals 1.8 bridge is active");
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            fail("EnhancedVisuals bridge initialization failed; using v1 fallback", exception);
            return false;
        }
    }

    public void tick(
            HealthFxState state,
            EffectChannels channels,
            HealthFxFrame frame,
            long clientTick
    ) {
        if (!active) {
            return;
        }
        try {
            screenChannelAvailable = (boolean) shouldRender.invoke(null);
            if (!screenChannelAvailable) {
                removeVisuals();
            } else if (HealthFxClientConfig.SCREEN_EFFECTS.get()
                    == HealthFxClientConfig.ScreenEffectsMode.OFF) {
                removeVisuals();
            } else {
                float modeScale = HealthFxClientConfig.SCREEN_EFFECTS.get()
                        == HealthFxClientConfig.ScreenEffectsMode.LOW ? 0.55F : 1.0F;
                updateSlot(SLOT_DAMAGED,
                        clampOpacity((frame.bloodAlpha() * 2.20D + frame.painAlpha() * 0.35D) * modeScale),
                        0);
                updateSlot(SLOT_LOW_HEALTH,
                        clampOpacity(frame.vignetteAlpha() * 2.65D * modeScale), 0);
                updateSlot(SLOT_TUNNEL,
                        clampOpacity(frame.painAlpha() * 2.85D * modeScale), 0);
                int crackVariant = Math.max(0, Math.min(12,
                        2 + state.fractureCount() * 4));
                updateSlot(SLOT_CRACK,
                        clampOpacity(frame.fractureAlpha() * 2.35D * modeScale), crackVariant);
            }
            tickAudio(state, channels, frame, clientTick);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            fail("EnhancedVisuals bridge failed at runtime; restoring v1 fallback", exception);
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean ownsScreenChannel() {
        return active && screenChannelAvailable;
    }

    public void reset() {
        removeVisuals();
        previousPain = 0.0D;
        nextHeartbeatTick = 0L;
        heartbeatInTick = -1L;
        screenChannelAvailable = false;
    }

    private void tickAudio(
            HealthFxState state,
            EffectChannels channels,
            HealthFxFrame frame,
            long clientTick
    ) throws ReflectiveOperationException {
        if (!HealthFxClientConfig.AUDIO_ENABLED.get()) {
            previousPain = state.visiblePain();
            heartbeatInTick = -1L;
            return;
        }
        float volumeScale = (float) (HealthFxClientConfig.EFFECT_VOLUME.get()
                * HealthFxClientConfig.MASTER_INTENSITY.get());
        if (volumeScale <= 0.001F) {
            previousPain = state.visiblePain();
            return;
        }

        double visiblePain = state.visiblePain();
        if (visiblePain >= previousPain + 8.0D) {
            play(damageHandler, "ringing-short",
                    InjuryAudioVolume.ringing(
                            (float) Math.min(0.62D,
                                    (0.20D + channels.pain() * 0.36D) * volumeScale),
                            HealthFxClientConfig.RINGING_VOLUME.get()));
        }
        previousPain = visiblePain;

        if (HealthFxClientConfig.DISABLE_HEARTBEAT.get()
                || frame.heartbeatVolume() <= 0.01D) {
            heartbeatInTick = -1L;
            return;
        }
        if (clientTick == heartbeatInTick) {
            play(heartbeatHandler, "heartbeatin",
                    (float) Math.min(0.72D, frame.heartbeatVolume() * volumeScale));
            heartbeatInTick = -1L;
        }
        if (clientTick >= nextHeartbeatTick) {
            play(heartbeatHandler, "heartbeatout",
                    (float) Math.min(0.72D, frame.heartbeatVolume() * volumeScale));
            heartbeatInTick = clientTick + 5L;
            long interval = Math.max(24L,
                    Math.round(52.0D - frame.heartbeatVolume() * 27.0D));
            nextHeartbeatTick = clientTick + interval;
        }
    }

    private void play(Object handler, String path, float volume)
            throws ReflectiveOperationException {
        if (volume > 0.001F) {
            playSound.invoke(handler,
                    new ResourceLocation("enhancedvisuals", path), volume);
        }
    }

    private void updateSlot(String key, float opacity, int variant)
            throws ReflectiveOperationException {
        VisualSlot slot = slots.get(key);
        if (slot == null || slot.type == null || slot.handler == null) {
            return;
        }
        if (opacity <= 0.004F) {
            remove(slot);
            return;
        }
        if (slot.visual == null) {
            slot.visual = endlessVisualConstructor.newInstance(slot.type, slot.handler, variant);
            visualAdd.invoke(null, slot.visual);
        }
        visualVariant.setInt(slot.visual, variant);
        setOpacity.invoke(slot.visual, opacity);
    }

    private void removeVisuals() {
        if (visualRemove == null) {
            return;
        }
        for (VisualSlot slot : slots.values()) {
            try {
                remove(slot);
            } catch (ReflectiveOperationException ignored) {
                // Best-effort cleanup while a foreign adapter is already failing.
            }
        }
    }

    private void remove(VisualSlot slot) throws ReflectiveOperationException {
        if (slot.visual != null) {
            visualRemove.invoke(null, slot.visual);
            slot.visual = null;
        }
    }

    private Object createCrackType(
            ClassLoader loader,
            Class<?> visualTypeClass,
            Minecraft minecraft
    ) throws ReflectiveOperationException {
        Method getTypes = visualTypeClass.getMethod("getTypes");
        Field nameField = visualTypeClass.getField("name");
        Object existing = ((Iterable<?>) getTypes.invoke(null)).iterator().hasNext()
                ? findType((Iterable<?>) getTypes.invoke(null), nameField, SLOT_CRACK)
                : null;
        if (existing != null) {
            return existing;
        }

        Class<?> overlayClass = Class.forName(
                "team.creative.enhancedvisuals.api.type.VisualTypeOverlay", false, loader);
        Object crack = overlayClass.getConstructor(String.class, int.class)
                .newInstance(SLOT_CRACK, 0);
        Method loadResources = visualTypeClass.getMethod(
                "loadResources", net.minecraft.server.packs.resources.ResourceManager.class);
        loadResources.invoke(crack, minecraft.getResourceManager());
        return crack;
    }

    private static Object findType(Iterable<?> types, Field nameField, String name)
            throws IllegalAccessException {
        for (Object type : types) {
            if (name.equals(nameField.get(type))) {
                return type;
            }
        }
        return null;
    }

    private static Object publicStaticField(Class<?> owner, String field)
            throws ReflectiveOperationException {
        return owner.getField(field).get(null);
    }

    private static Object publicField(Object owner, String field)
            throws ReflectiveOperationException {
        return owner.getClass().getField(field).get(owner);
    }

    private static float clampOpacity(double value) {
        return (float) Math.max(0.0D, Math.min(0.62D, value));
    }

    private void fail(String message, Throwable exception) {
        removeVisuals();
        active = false;
        screenChannelAvailable = false;
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn(message, exception);
        }
    }

    private static final class VisualSlot {
        private final Object type;
        private final Object handler;
        private Object visual;

        private VisualSlot(Object type, Object handler) {
            this.type = type;
            this.handler = handler;
        }
    }
}
