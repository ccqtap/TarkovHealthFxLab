package com.qq.tarkovhealthfxlab.compat.cameraoverhaul;

import com.mojang.logging.LogUtils;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Optional Camera Overhaul 1.1 callback integration. */
public final class CameraOverhaulBridge {
    private static final Logger LOGGER = LogUtils.getLogger();

    private volatile double pitch;
    private volatile double roll;
    private Field eulerRotation;
    private Object callback;
    private boolean active;
    private boolean failureLogged;

    public boolean initialize() {
        if (active) {
            return true;
        }
        try {
            ClassLoader loader = CameraOverhaulBridge.class.getClassLoader();
            Class<?> callbackClass = Class.forName(
                    "mirsario.cameraoverhaul.core.callbacks.ModifyCameraTransformCallback",
                    false,
                    loader
            );
            Class<?> transformClass = Class.forName(
                    "mirsario.cameraoverhaul.core.structures.Transform", false, loader);
            eulerRotation = transformClass.getField("eulerRot");

            callback = Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass},
                    (proxy, method, arguments) -> invoke(proxy, method, arguments)
            );
            Object event = callbackClass.getField("EVENT").get(null);
            Method register = event.getClass().getMethod("Register", Object.class);
            register.invoke(event, callback);
            active = true;
            LOGGER.info("Tarkov Health FX: Camera Overhaul 1.1 callback is active");
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            fail(exception);
            return false;
        }
    }

    public void update(double pitch, double roll) {
        this.pitch = finiteOrZero(pitch);
        this.roll = finiteOrZero(roll);
    }

    public void reset() {
        update(0.0D, 0.0D);
    }

    public boolean isActive() {
        return active;
    }

    private Object invoke(Object proxy, Method method, Object[] arguments) {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "TarkovHealthFxCameraCallback";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == (arguments == null ? null : arguments[0]);
                default -> null;
            };
        }
        if (!"ModifyCameraTransform".equals(method.getName())
                || arguments == null
                || arguments.length < 2
                || arguments[1] == null) {
            return arguments != null && arguments.length > 1 ? arguments[1] : null;
        }

        Object transform = arguments[1];
        if (!active) {
            return transform;
        }
        try {
            Vec3 rotation = (Vec3) eulerRotation.get(transform);
            if (rotation != null) {
                eulerRotation.set(transform, rotation.add(pitch, 0.0D, roll));
            }
        } catch (IllegalAccessException | RuntimeException exception) {
            // A callback must never be allowed to take down the render loop.
            fail(exception);
        }
        return transform;
    }

    private void fail(Throwable exception) {
        active = false;
        reset();
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn("Camera Overhaul callback could not be installed; using Forge camera fallback",
                    exception);
        }
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }
}
