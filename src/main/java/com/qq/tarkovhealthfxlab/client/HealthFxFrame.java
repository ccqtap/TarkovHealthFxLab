package com.qq.tarkovhealthfxlab.client;

public record HealthFxFrame(
        double bloodAlpha,
        double fractureAlpha,
        double painAlpha,
        double vignetteAlpha,
        double heartbeatVolume,
        double breathVolume,
        double cameraPitch,
        double cameraRoll
) {
}
