package com.qq.tarkovhealthfxlab.client.audio;

/** Pure channel scaling shared by built-in and third-party audio providers. */
public final class InjuryAudioVolume {
    private InjuryAudioVolume() {
    }

    public static float ringing(float volume, double ringingSetting) {
        double safeVolume = Math.max(0.0D, volume);
        double safeSetting = Math.max(0.0D, Math.min(1.0D, ringingSetting));
        return (float) (safeVolume * safeSetting);
    }
}
