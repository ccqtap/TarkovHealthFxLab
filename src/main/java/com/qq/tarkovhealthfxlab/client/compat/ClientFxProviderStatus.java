package com.qq.tarkovhealthfxlab.client.compat;

import java.util.Objects;

/** Names the providers currently owning each client feedback channel. */
public record ClientFxProviderStatus(
        String visuals,
        String audio,
        String camera,
        String analgesiaCompatibility,
        String tacz,
        String lrTactical
) {
    public ClientFxProviderStatus {
        visuals = clean(visuals);
        audio = clean(audio);
        camera = clean(camera);
        analgesiaCompatibility = clean(analgesiaCompatibility);
        tacz = clean(tacz);
        lrTactical = clean(lrTactical);
    }

    public static ClientFxProviderStatus uninitialized() {
        return new ClientFxProviderStatus("pending", "pending", "pending", "pending",
                "pending", "pending");
    }

    public String compactDisplay() {
        String screenAndAudio = visuals.equals(audio)
                ? "Visual/Audio: " + visuals
                : "Visual: " + visuals + " | Audio: " + audio;
        return screenAndAudio + " | Camera: " + camera + " | Explosion: "
                + analgesiaCompatibility + " | TaCZ: " + tacz + " | LR: " + lrTactical;
    }

    public String visualProviderDisplay() {
        return "EnhancedVisuals: " + visuals + " | Camera: " + camera
                + " | Explosion: " + analgesiaCompatibility;
    }

    public String externalProviderDisplay() {
        return "TaCZ: " + tacz + " | LR Tactical: " + lrTactical;
    }

    private static String clean(String value) {
        String cleaned = Objects.requireNonNullElse(value, "unknown").trim();
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }
}
