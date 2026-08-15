package com.qq.tarkovhealthfxlab.client.compat;

import java.util.Locale;
import java.util.Objects;

/**
 * Pure compatibility selection. Keeping version decisions out of the reflective
 * adapters makes an unknown third-party update fall back instead of crashing the
 * client during class loading.
 */
public final class ClientFxCompatPolicy {
    public static final String ENHANCED_VISUALS_ID = "enhancedvisuals";
    public static final String CAMERA_OVERHAUL_ID = "cameraoverhaul";
    public static final String EXPLOSION_OVERHAUL_ID = "explosionoverhaul";

    private ClientFxCompatPolicy() {
    }

    public static Selection select(
            ModCandidate enhancedVisuals,
            ModCandidate cameraOverhaul,
            ModCandidate explosionOverhaul
    ) {
        Objects.requireNonNull(enhancedVisuals, "enhancedVisuals");
        Objects.requireNonNull(cameraOverhaul, "cameraOverhaul");
        Objects.requireNonNull(explosionOverhaul, "explosionOverhaul");
        return new Selection(
                supportsEnhancedVisuals(enhancedVisuals),
                supportsCameraOverhaul(cameraOverhaul),
                supportsExplosionOverhaul(explosionOverhaul)
        );
    }

    static boolean supportsEnhancedVisuals(ModCandidate candidate) {
        return candidate.loaded() && normalized(candidate.version()).startsWith("1.8.");
    }

    static boolean supportsCameraOverhaul(ModCandidate candidate) {
        String version = normalized(candidate.version());
        return candidate.loaded()
                && (version.equals("1.1") || version.startsWith("1.1-1.20.1"));
    }

    static boolean supportsExplosionOverhaul(ModCandidate candidate) {
        // The adapter intentionally reaches a few private 0.2.3 fields in order
        // to cancel already queued camera shakes. Never run that adapter against
        // a different implementation layout.
        return candidate.loaded()
                && normalized(candidate.version()).equals("0.2.3.0-forge");
    }

    public static boolean shouldSuppressExplosion(Selection selection, boolean analgesiaActive) {
        return selection.explosionOverhaul() && analgesiaActive;
    }

    /**
     * EnhancedVisuals can be installed and API-compatible while its renderer
     * is intentionally disabled for the current player (creative/spectator).
     * In that state it must not retain ownership of the screen channel or the
     * player would receive neither the reused visuals nor the built-in fallback.
     */
    public static boolean shouldUseLegacyScreenEffects(
            boolean enhancedBridgeActive,
            boolean enhancedRendererAvailable
    ) {
        return !enhancedBridgeActive || !enhancedRendererAvailable;
    }

    private static String normalized(String value) {
        return Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);
    }

    public record ModCandidate(boolean loaded, String version) {
        public ModCandidate {
            version = Objects.requireNonNullElse(version, "");
        }

        public static ModCandidate missing() {
            return new ModCandidate(false, "");
        }
    }

    public record Selection(
            boolean enhancedVisuals,
            boolean cameraOverhaul,
            boolean explosionOverhaul
    ) {
    }
}
