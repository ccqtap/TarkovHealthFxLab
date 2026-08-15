package com.qq.tarkovhealthfxlab.client.compat;

import com.qq.tarkovhealthfxlab.client.EffectChannels;
import com.qq.tarkovhealthfxlab.client.HealthFxClientConfig;
import com.qq.tarkovhealthfxlab.client.HealthFxFrame;
import com.qq.tarkovhealthfxlab.client.HealthFxState;
import com.qq.tarkovhealthfxlab.compat.cameraoverhaul.CameraOverhaulBridge;
import com.qq.tarkovhealthfxlab.compat.enhancedvisuals.EnhancedVisualsBridge;
import com.qq.tarkovhealthfxlab.compat.explosionoverhaul.ExplosionOverhaul023Bridge;
import com.qq.tarkovhealthfxlab.compat.ThirdPartyCompatBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModContainer;

/** Owns client provider selection so a channel is never rendered twice. */
public final class ClientFxCompatCoordinator {
    private static final EnhancedVisualsBridge ENHANCED_VISUALS = new EnhancedVisualsBridge();
    private static final CameraOverhaulBridge CAMERA_OVERHAUL = new CameraOverhaulBridge();
    private static final ExplosionOverhaul023Bridge EXPLOSION_OVERHAUL =
            new ExplosionOverhaul023Bridge();

    private static boolean initialized;
    private static ClientFxCompatPolicy.Selection selection =
            new ClientFxCompatPolicy.Selection(false, false, false);
    private static ClientFxCompatPolicy.ModCandidate enhancedCandidate =
            ClientFxCompatPolicy.ModCandidate.missing();
    private static ClientFxCompatPolicy.ModCandidate cameraCandidate =
            ClientFxCompatPolicy.ModCandidate.missing();
    private static ClientFxCompatPolicy.ModCandidate explosionCandidate =
            ClientFxCompatPolicy.ModCandidate.missing();
    private static ClientFxProviderStatus providerStatus =
            ClientFxProviderStatus.uninitialized();

    private ClientFxCompatCoordinator() {
    }

    public static void tick(
            Minecraft minecraft,
            HealthFxState state,
            EffectChannels channels,
            HealthFxFrame frame,
            long clientTick
    ) {
        ensureInitialized(minecraft);

        if (ENHANCED_VISUALS.isActive()) {
            ENHANCED_VISUALS.tick(state, channels, frame, clientTick);
        }

        if (CAMERA_OVERHAUL.isActive()) {
            boolean allowCamera = minecraft.player != null
                    && minecraft.options.getCameraType().isFirstPerson()
                    && !HealthFxClientConfig.REDUCE_MOTION.get()
                    && HealthFxClientConfig.SCREEN_EFFECTS.get()
                    != HealthFxClientConfig.ScreenEffectsMode.OFF;
            CAMERA_OVERHAUL.update(
                    allowCamera ? frame.cameraPitch() : 0.0D,
                    allowCamera ? frame.cameraRoll() : 0.0D
            );
        }

        if (ClientFxCompatPolicy.shouldSuppressExplosion(
                selection, state.painkillerActive())
                && EXPLOSION_OVERHAUL.isActive()) {
            EXPLOSION_OVERHAUL.suppress(minecraft);
        }
        refreshProviderStatus();
    }

    public static void reset() {
        ENHANCED_VISUALS.reset();
        CAMERA_OVERHAUL.reset();
    }

    public static boolean useLegacyScreenEffects() {
        return ClientFxCompatPolicy.shouldUseLegacyScreenEffects(
                ENHANCED_VISUALS.isActive(),
                ENHANCED_VISUALS.ownsScreenChannel()
        );
    }

    public static boolean useLegacyHeartbeatAndPainAudio() {
        return !ENHANCED_VISUALS.isActive();
    }

    public static boolean useForgeCameraFallback() {
        return !CAMERA_OVERHAUL.isActive();
    }

    public static ClientFxProviderStatus providerStatus() {
        return providerStatus;
    }

    private static synchronized void ensureInitialized(Minecraft minecraft) {
        if (initialized) {
            return;
        }
        enhancedCandidate = probe(ClientFxCompatPolicy.ENHANCED_VISUALS_ID);
        cameraCandidate = probe(ClientFxCompatPolicy.CAMERA_OVERHAUL_ID);
        explosionCandidate = probe(ClientFxCompatPolicy.EXPLOSION_OVERHAUL_ID);
        selection = ClientFxCompatPolicy.select(
                enhancedCandidate, cameraCandidate, explosionCandidate);

        if (selection.enhancedVisuals()) {
            ENHANCED_VISUALS.initialize(minecraft);
        }
        if (selection.cameraOverhaul()) {
            CAMERA_OVERHAUL.initialize();
        }
        if (selection.explosionOverhaul()) {
            EXPLOSION_OVERHAUL.initialize();
        }
        initialized = true;
        refreshProviderStatus();
    }

    private static ClientFxCompatPolicy.ModCandidate probe(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(ClientFxCompatCoordinator::candidate)
                .orElseGet(ClientFxCompatPolicy.ModCandidate::missing);
    }

    private static ClientFxCompatPolicy.ModCandidate candidate(ModContainer container) {
        return new ClientFxCompatPolicy.ModCandidate(
                true,
                container.getModInfo().getVersion().toString()
        );
    }

    private static void refreshProviderStatus() {
        String visual;
        if (ENHANCED_VISUALS.ownsScreenChannel()) {
            visual = named("EnhancedVisuals", enhancedCandidate);
        } else if (ENHANCED_VISUALS.isActive()) {
            visual = "built-in v1 fallback (EnhancedVisuals renderer disabled)";
        } else {
            visual = fallback(enhancedCandidate, selection.enhancedVisuals());
        }
        String audio = ENHANCED_VISUALS.isActive()
                ? named("EnhancedVisuals", enhancedCandidate)
                : fallback(enhancedCandidate, selection.enhancedVisuals());
        String camera = CAMERA_OVERHAUL.isActive()
                ? named("Camera Overhaul", cameraCandidate)
                : fallback(cameraCandidate, selection.cameraOverhaul());
        String analgesia = EXPLOSION_OVERHAUL.isActive()
                ? named("Explosion Overhaul", explosionCandidate)
                : unavailable(explosionCandidate, selection.explosionOverhaul());
        ThirdPartyCompatBootstrap.Availability external =
                ThirdPartyCompatBootstrap.availability();
        providerStatus = new ClientFxProviderStatus(
                visual,
                audio,
                camera,
                analgesia,
                directAdapterStatus("tacz", external.tacz()),
                directAdapterStatus("lrtactical", external.lrTactical())
        );
    }

    private static String named(String name, ClientFxCompatPolicy.ModCandidate candidate) {
        return candidate.version().isBlank() ? name : name + " " + candidate.version();
    }

    private static String fallback(
            ClientFxCompatPolicy.ModCandidate candidate,
            boolean versionSelected
    ) {
        if (!candidate.loaded()) {
            return "built-in v1 fallback";
        }
        return versionSelected ? "built-in v1 fallback (bridge failed)"
                : "built-in v1 fallback (unsupported " + candidate.version() + ")";
    }

    private static String unavailable(
            ClientFxCompatPolicy.ModCandidate candidate,
            boolean versionSelected
    ) {
        if (!candidate.loaded()) {
            return "not installed";
        }
        return versionSelected ? "bridge failed"
                : "unsupported " + candidate.version();
    }

    private static String directAdapterStatus(String modId, boolean active) {
        if (!ModList.get().isLoaded(modId)) {
            return "not installed";
        }
        if (active) {
            return "adapter active";
        }
        String installedVersion = ThirdPartyCompatBootstrap.installedVersion(modId);
        if (!ThirdPartyCompatBootstrap.supportedInstalledVersion(modId)) {
            return "unsupported " + installedVersion;
        }
        return ThirdPartyCompatBootstrap.installAttempted()
                ? "installed; adapter absent/failed"
                : "pending";
    }
}
