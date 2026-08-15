package com.qq.tarkovhealthfxlab.compat.explosionoverhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Version-pinned analgesia adapter for Explosion Overhaul 0.2.3.0-forge.
 * Public stop methods cancel concussion systems; the exact-version private
 * fields cancel its separately queued generic shake without copying its code.
 */
public final class ExplosionOverhaul023Bridge {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Method stopBlur;
    private Method stopConcussionCamera;
    private Method stopConcussionAudio;
    private Method stopDeafness;
    private Method stopLowPass;
    private Field shakeIntensity;
    private Field shakeDuration;
    private Field pushIntensity;
    private Field lastYawOffset;
    private Field lastPitchOffset;
    private Field pendingShakes;
    private boolean active;
    private boolean failureLogged;

    public boolean initialize() {
        if (active) {
            return true;
        }
        try {
            ClassLoader loader = ExplosionOverhaul023Bridge.class.getClassLoader();
            stopBlur = stopMethod(loader,
                    "com.vinlanx.explosionoverhaul.client.Blur", "stop");
            stopConcussionCamera = stopMethod(loader,
                    "com.vinlanx.explosionoverhaul.client.CameraShakeConcussionEffect", "stop");
            stopConcussionAudio = stopMethod(loader,
                    "com.vinlanx.explosionoverhaul.client.ConcussionAudioEffect", "stopAll");
            stopDeafness = stopMethod(loader,
                    "com.vinlanx.explosionoverhaul.client.DeafnessConcussionEffect", "stop");
            stopLowPass = stopMethod(loader,
                    "com.vinlanx.explosionoverhaul.client.LowPassConcussionEffect", "stop");

            Class<?> effects = Class.forName(
                    "com.vinlanx.explosionoverhaul.client.ClientEffects", false, loader);
            shakeIntensity = privateStaticField(effects, "currentShakeIntensity");
            shakeDuration = privateStaticField(effects, "shakeDurationTicks");
            pushIntensity = privateStaticField(effects, "currentPushIntensity");
            lastYawOffset = privateStaticField(effects, "lastYawOffset");
            lastPitchOffset = privateStaticField(effects, "lastPitchOffset");
            pendingShakes = privateStaticField(effects, "pendingShakes");
            active = true;
            LOGGER.info("Tarkov Health FX: Explosion Overhaul 0.2.3 analgesia bridge is active");
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            fail(exception);
            return false;
        }
    }

    public void suppress(Minecraft minecraft) {
        if (!active) {
            return;
        }
        try {
            stopBlur.invoke(null);
            stopConcussionCamera.invoke(null);
            stopConcussionAudio.invoke(null);
            stopDeafness.invoke(null);
            stopLowPass.invoke(null);

            float yaw = lastYawOffset.getFloat(null);
            float pitch = lastPitchOffset.getFloat(null);
            if (minecraft.player != null && (yaw != 0.0F || pitch != 0.0F)) {
                // ClientEffects normally removes its prior offset on the next
                // tick. Analgesia cancels immediately, so restore it here first.
                minecraft.player.turn(-yaw, -pitch);
            }
            lastYawOffset.setFloat(null, 0.0F);
            lastPitchOffset.setFloat(null, 0.0F);
            shakeIntensity.setFloat(null, 0.0F);
            shakeDuration.setInt(null, 0);
            pushIntensity.setFloat(null, 0.0F);
            Object queued = pendingShakes.get(null);
            if (queued instanceof List<?> list) {
                list.clear();
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            fail(exception);
        }
    }

    public boolean isActive() {
        return active;
    }

    private static Method stopMethod(ClassLoader loader, String className, String methodName)
            throws ReflectiveOperationException {
        return Class.forName(className, false, loader).getMethod(methodName);
    }

    private static Field privateStaticField(Class<?> owner, String name)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private void fail(Throwable exception) {
        active = false;
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn("Explosion Overhaul 0.2.3 analgesia bridge failed safely", exception);
        }
    }
}
