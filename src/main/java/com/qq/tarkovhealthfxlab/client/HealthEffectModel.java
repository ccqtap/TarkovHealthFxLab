package com.qq.tarkovhealthfxlab.client;

/** Pure, deterministic mapping from injury channels to one audiovisual frame. */
public final class HealthEffectModel {
    private HealthEffectModel() {
    }

    public static HealthFxFrame evaluate(
            EffectChannels channels,
            double timeSeconds,
            boolean moving,
            boolean onGround,
            double masterIntensity,
            boolean lowScreenEffects,
            boolean reduceMotion,
            double cameraIntensity
    ) {
        double master = EffectChannels.clamp01(masterIntensity);
        double modeScale = lowScreenEffects ? 0.55D : 1.0D;
        double bleedPulse = pulse(timeSeconds, channels.bleeding() > 0.7D ? 1.0D : 7.5D, 3.2D);
        double painPeriod = lerp(7.0D, 2.8D, channels.pain());
        double painWave = pulse(timeSeconds + 0.37D, painPeriod, 2.6D);
        double step = moving && onGround && channels.fracturedLegs() > 0
                ? pulse(timeSeconds, 0.62D, 6.0D)
                : 0.0D;

        double bloodAlpha = master * modeScale * channels.bleeding()
                * (0.075D + 0.075D * bleedPulse);
        double fractureAlpha = master * modeScale * channels.fracture()
                * (0.085D + 0.045D * step);
        double painAlpha = master * modeScale * channels.pain()
                * (0.055D + 0.080D * painWave);
        double vignetteAlpha = master * modeScale
                * Math.min(0.20D, channels.critical() * 0.17D + channels.pain() * 0.045D);

        double heartbeat = master * Math.max(channels.critical() * 0.65D, channels.bleeding() * 0.75D);
        double breath = master * channels.pain() * (0.35D + 0.65D * painWave);

        double pitch = 0.0D;
        double roll = 0.0D;
        if (!reduceMotion) {
            double camera = EffectChannels.clamp01(cameraIntensity);
            // Pain uses two low-amplitude, non-matching waves so it feels like
            // unstable focus rather than weapon recoil or screen vibration.
            double painMotion = camera * channels.pain();
            pitch += painMotion * (0.040D * Math.sin(timeSeconds * 8.7D)
                    + 0.018D * Math.sin(timeSeconds * 15.1D + 0.8D));
            roll += painMotion * 0.040D * Math.sin(timeSeconds * 6.3D + 1.1D);

            if (moving && onGround && channels.fracturedLegs() > 0) {
                pitch += -0.18D * camera * channels.fracture() * step;
                roll += 0.24D * camera * channels.fracture()
                        * Math.sin(timeSeconds * Math.PI * 2.0D / 1.24D);
            }
            pitch = clamp(pitch, -0.22D, 0.22D);
            roll = clamp(roll, -0.28D, 0.28D);
        }
        return new HealthFxFrame(
                clampAlpha(bloodAlpha),
                clampAlpha(fractureAlpha),
                clampAlpha(painAlpha),
                clampAlpha(vignetteAlpha),
                EffectChannels.clamp01(heartbeat),
                EffectChannels.clamp01(breath),
                pitch,
                roll
        );
    }

    private static double pulse(double timeSeconds, double periodSeconds, double exponent) {
        if (!Double.isFinite(timeSeconds) || periodSeconds <= 0.0D) {
            return 0.0D;
        }
        double phase = (timeSeconds % periodSeconds) / periodSeconds;
        double wave = 0.5D - 0.5D * Math.cos(phase * Math.PI * 2.0D);
        return Math.pow(wave, exponent);
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * EffectChannels.clamp01(amount);
    }

    private static double clampAlpha(double value) {
        return Math.max(0.0D, Math.min(0.28D, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
