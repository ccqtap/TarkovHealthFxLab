package com.qq.tarkovhealthfxlab.client;

import com.qq.tarkovhealthfxlab.client.audio.HealthSoundController;
import com.qq.tarkovhealthfxlab.client.compat.ClientFxCompatCoordinator;
import com.qq.tarkovhealthfxlab.client.compat.ClientFxProviderStatus;
import com.qq.tarkovhealthfxlab.compat.tarkov.TarkovLiveStateProvider;
import com.qq.tarkovhealthfxlab.network.ClientInjuryState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

public final class HealthFxController {
    private static final int DEFAULT_ANALGESIA_SECONDS = 60;
    private static final int DEFAULT_REGENERATION_SECONDS = 20;

    private static HealthFxSource source = HealthFxSource.MOCK;
    private static BodyRegion selectedRegion = BodyRegion.LEFT_LEG;
    private static HealthFxState mockState = HealthFxState.healthy();
    private static HealthFxState displayedState = mockState;
    private static ClientInjuryState.Snapshot labSnapshot = ClientInjuryState.Snapshot.empty();
    private static EffectChannels smoothedChannels = EffectChannels.from(mockState);
    private static String liveStatusKey = "healthfx.live.mod_missing";
    private static long clientTicks;

    private HealthFxController() {
    }

    public static void tick(Minecraft minecraft) {
        clientTicks++;
        LocalPlayer player = minecraft.player;
        switch (source) {
            case MOCK -> {
                displayedState = mockState;
                liveStatusKey = "healthfx.live.mock";
            }
            case LAB_SERVER -> sampleLabServer(player);
            case TARKOV_LIVE -> {
                TarkovLiveStateProvider.Result result = TarkovLiveStateProvider.sample(player);
                liveStatusKey = result.statusKey();
                displayedState = result.state().orElse(HealthFxState.healthy());
            }
        }

        EffectChannels target = EffectChannels.from(displayedState);
        smoothedChannels = EffectChannels.interpolate(smoothedChannels, target, 0.14D);
        boolean moving = isMoving(player);
        boolean onGround = player != null && player.onGround();
        ClientFxCompatCoordinator.tick(
                minecraft,
                displayedState,
                smoothedChannels,
                frame(0.0F),
                clientTicks
        );
        HealthSoundController.tick(minecraft, displayedState, smoothedChannels, moving, onGround, clientTicks);
    }

    private static void sampleLabServer(LocalPlayer player) {
        if (player == null) {
            labSnapshot = ClientInjuryState.Snapshot.empty();
            displayedState = HealthFxState.healthy();
            liveStatusKey = "healthfx.live.no_player";
            return;
        }
        labSnapshot = ClientInjuryState.get(player.getUUID());
        displayedState = LabInjuryStateMapper.fromSnapshot(labSnapshot);
        liveStatusKey = "healthfx.lab.connected";
    }

    public static HealthFxFrame frame(float partialTick) {
        double timeSeconds = (clientTicks + Math.max(0.0F, partialTick)) / 20.0D;
        LocalPlayer player = Minecraft.getInstance().player;
        return HealthEffectModel.evaluate(
                smoothedChannels,
                timeSeconds,
                isMoving(player),
                player != null && player.onGround(),
                HealthFxClientConfig.MASTER_INTENSITY.get(),
                HealthFxClientConfig.SCREEN_EFFECTS.get() == HealthFxClientConfig.ScreenEffectsMode.LOW,
                HealthFxClientConfig.REDUCE_MOTION.get(),
                HealthFxClientConfig.CAMERA_INTENSITY.get()
        );
    }

    public static void resetSession() {
        displayedState = HealthFxState.healthy();
        labSnapshot = ClientInjuryState.Snapshot.empty();
        smoothedChannels = EffectChannels.from(displayedState);
        clientTicks = 0L;
        HeadRedirectClientStatus.reset();
        ClientFxCompatCoordinator.reset();
        HealthSoundController.reset();
    }

    public static HealthFxSource source() {
        return source;
    }

    public static void setSource(HealthFxSource value) {
        source = value == null ? HealthFxSource.MOCK : value;
        if (source == HealthFxSource.LAB_SERVER) {
            requestHeadRedirectStatus();
        }
    }

    public static void cycleSource() {
        setSource(source.next());
    }

    public static BodyRegion selectedRegion() {
        return selectedRegion;
    }

    public static void setSelectedRegion(BodyRegion region) {
        selectedRegion = region == null ? BodyRegion.STOMACH : region;
    }

    public static void cycleSelectedRegion() {
        selectedRegion = selectedRegion.next();
    }

    public static HealthFxState displayedState() {
        return displayedState;
    }

    public static HealthFxState mockState() {
        return mockState;
    }

    public static ClientInjuryState.Snapshot labSnapshot() {
        return labSnapshot.copy();
    }

    public static BodyEffect selectedMockEffect() {
        return mockState.effect(selectedRegion);
    }

    public static BodyEffect selectedEffect() {
        return source == HealthFxSource.MOCK ? selectedMockEffect() : displayedState.effect(selectedRegion);
    }

    public static String liveStatusKey() {
        return liveStatusKey;
    }

    public static ClientFxProviderStatus providerStatus() {
        return ClientFxCompatCoordinator.providerStatus();
    }

    public static void applyPreset(HealthFxPreset preset) {
        source = HealthFxSource.MOCK;
        mockState = preset.create(mockState.revision() + 1L);
        displayedState = mockState;
    }

    /** Applies the button as a local preset or an authoritative server scenario without changing source. */
    public static boolean applyScene(HealthFxPreset preset) {
        if (source == HealthFxSource.MOCK) {
            mockState = preset.create(mockState.revision() + 1L);
            displayedState = mockState;
            return true;
        }
        if (source != HealthFxSource.LAB_SERVER || !serverControlAvailable()) {
            return false;
        }
        String target = selfTarget();
        for (String command : LabServerScenario.commands(preset, target)) {
            if (!sendServerCommand(command)) return false;
        }
        return true;
    }

    public static void cycleBleeding() {
        BodyEffect before = selectedEffect();
        setBleeding(before.bleeding().next());
    }

    public static void setBleeding(BleedingLevel level) {
        if (source == HealthFxSource.MOCK) {
            mockState = mockState.withEffect(selectedRegion, selectedMockEffect().withBleeding(level));
        } else if (source == HealthFxSource.LAB_SERVER) {
            sendServerCommand("set bleeding " + selfTarget() + " " + selectedRegion.id() + " "
                    + level.name().toLowerCase(Locale.ROOT));
        }
    }

    public static void toggleFracture() {
        setFracture(!selectedEffect().fractured());
    }

    public static void setFracture(boolean fractured) {
        if (!isSelectedLimb()) return;
        if (source == HealthFxSource.MOCK) {
            mockState = mockState.withEffect(selectedRegion, selectedMockEffect().withFractured(fractured));
        } else if (source == HealthFxSource.LAB_SERVER) {
            sendServerCommand("set fracture " + selfTarget() + " " + selectedRegion.id() + " " + fractured);
        }
    }

    public static void toggleBlackened() {
        setBlackened(!selectedEffect().blackened());
    }

    public static void setBlackened(boolean blackened) {
        if (!isSelectedLimb()) return;
        if (source == HealthFxSource.MOCK) {
            mockState = mockState.withEffect(selectedRegion, selectedMockEffect().withBlackened(blackened));
        } else if (source == HealthFxSource.LAB_SERVER) {
            sendServerCommand("set blackened " + selfTarget() + " " + selectedRegion.id() + " " + blackened);
        }
    }

    public static void setPain(double pain) {
        double safe = Math.max(0.0D, Math.min(100.0D, pain));
        if (source == HealthFxSource.MOCK) {
            mockState = mockState.withEffect(selectedRegion, selectedMockEffect().withPain(safe));
        } else if (source == HealthFxSource.LAB_SERVER) {
            sendServerCommand("set pain " + selfTarget() + " " + selectedRegion.id() + " " + oneDecimal(safe));
        }
    }

    /** Legacy aggregate mock control retained for the v1 client command. */
    public static void setHealthRatio(double ratio) {
        requireMock();
        mockState = mockState.withHealthRatio(Math.max(0.0D, Math.min(1.0D, ratio)));
    }

    public static void setSelectedPartHealthRatio(double ratio) {
        BodyEffect before = selectedEffect();
        double safe = Math.max(0.0D, Math.min(1.0D, ratio));
        if (source == HealthFxSource.MOCK) {
            BodyEffect updated = MockInjuryMutations.setPartHealthRatio(selectedRegion, before, safe);
            mockState = mockState.withEffect(selectedRegion, updated);
        } else if (source == HealthFxSource.LAB_SERVER) {
            sendServerCommand("set part_hp " + selfTarget() + " " + selectedRegion.id() + " "
                    + oneDecimal(before.maximumHealth() * safe));
        }
    }

    public static void togglePainkiller() {
        if (source == HealthFxSource.MOCK) {
            mockState = mockState.withPainkillerActive(!mockState.painkillerActive());
        } else if (source == HealthFxSource.LAB_SERVER) {
            requestAnalgesia(!labSnapshot.analgesia());
        }
    }

    public static boolean requestRepair() {
        return source == HealthFxSource.LAB_SERVER && sendServerCommand("repair " + selfTarget());
    }

    public static boolean requestAnalgesia(boolean enabled) {
        if (source == HealthFxSource.MOCK) {
            if (mockState.painkillerActive() != enabled) togglePainkiller();
            return true;
        }
        if (source != HealthFxSource.LAB_SERVER) return false;
        String command = enabled
                ? "analgesia on " + selfTarget() + " " + DEFAULT_ANALGESIA_SECONDS
                : "analgesia off " + selfTarget();
        return sendServerCommand(command);
    }

    public static boolean requestRegeneration() {
        return source == HealthFxSource.LAB_SERVER
                && sendServerCommand("regeneration " + selfTarget() + " " + DEFAULT_REGENERATION_SECONDS);
    }

    public static boolean requestClear() {
        if (source == HealthFxSource.MOCK) {
            applyPreset(HealthFxPreset.OFF);
            return true;
        }
        return source == HealthFxSource.LAB_SERVER && sendServerCommand("clear " + selfTarget());
    }

    public static boolean requestHeadRedirectStatus() {
        return source == HealthFxSource.LAB_SERVER && sendServerCommand("head_redirect status");
    }

    public static boolean requestHeadRedirectToggle() {
        if (source != HealthFxSource.LAB_SERVER) return false;
        return switch (HeadRedirectClientStatus.state()) {
            case ENABLED -> sendServerCommand("head_redirect off");
            case DISABLED -> sendServerCommand("head_redirect on");
            case UNKNOWN -> requestHeadRedirectStatus();
        };
    }

    public static boolean requestHeadRedirectTest() {
        return source == HealthFxSource.LAB_SERVER
                && sendServerCommand("head_redirect test 20 7");
    }

    public static boolean acceptServerFeedback(String message) {
        return HeadRedirectClientStatus.accept(message);
    }

    public static boolean isMock() {
        return source == HealthFxSource.MOCK;
    }

    public static boolean isEditable() {
        return source == HealthFxSource.MOCK
                || source == HealthFxSource.LAB_SERVER && serverControlAvailable();
    }

    public static boolean serverControlAvailable() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.connection != null && player.hasPermissions(2);
    }

    private static boolean isSelectedLimb() {
        return selectedRegion.isArm() || selectedRegion.isLeg();
    }

    private static boolean sendServerCommand(String serverArguments) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null || !player.hasPermissions(2)) {
            liveStatusKey = "healthfx.lab.permission_required";
            return false;
        }
        player.connection.sendCommand(serverCommandLine(serverArguments));
        liveStatusKey = "healthfx.lab.command_sent";
        return true;
    }

    static String serverCommandLine(String serverArguments) {
        return "healthfx_server " + serverArguments;
    }

    private static String selfTarget() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? "@s" : player.getScoreboardName();
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static boolean isMoving(LocalPlayer player) {
        return player != null && player.getDeltaMovement().horizontalDistanceSqr() > 0.0004D;
    }

    private static void requireMock() {
        if (source != HealthFxSource.MOCK) {
            throw new IllegalStateException("mock controls are unavailable while a synchronized source is selected");
        }
    }
}
