package com.qq.tarkovhealthfxlab.common.health;

import com.qq.tarkovhealthfxlab.common.effect.InjuryEffectBridge;
import com.qq.tarkovhealthfxlab.common.effect.ModEffects;
import com.qq.tarkovhealthfxlab.network.HealthFxNetwork;
import com.qq.tarkovhealthfxlab.network.InjuryStateSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.util.Objects;
import java.util.function.Consumer;

/** Stable common API for TaCZ, LR Tactical, commands, and the future Tarkov adapter. */
public final class HealthRuleService {
    private HealthRuleService() {
    }

    public static InjuryState get(Player player) {
        return PlayerInjuryStore.get(player).copy();
    }

    public static void setPartHealth(ServerPlayer player, BodyPart part, float health) {
        update(player, state -> state.setHealth(part, health));
    }

    public static void setMaximumPartHealth(ServerPlayer player, BodyPart part, float maximum) {
        update(player, state -> state.setMaximumHealth(part, maximum));
    }

    /**
     * Applies part damage only; the caller remains responsible for vanilla/Tarkov entity damage.
     * HEAD is redirected when the persistent server switch is enabled.
     */
    public static DamageApplication damagePart(ServerPlayer player, BodyPart originalPart, float amount, long seed) {
        HeadDamageRedirectSavedData redirect = HeadDamageRedirectSavedData.get(player.server);
        DamageApplication application = originalPart == BodyPart.HEAD
                ? redirect.resolveAndRecord(originalPart, amount, seed)
                : HeadDamageRedirector.resolve(false, originalPart, amount, seed);
        update(player, state -> state.damage(application.appliedPart(), amount));
        return application;
    }

    public static float healPart(ServerPlayer player, BodyPart part, float amount, HealingSource source) {
        float[] healed = new float[1];
        update(player, state -> healed[0] = state.heal(part, amount, source));
        return healed[0];
    }

    public static void setBleeding(ServerPlayer player, BodyPart part, BleedingSeverity severity) {
        update(player, state -> state.setBleeding(part, severity));
    }

    public static void setPain(ServerPlayer player, BodyPart part, float pain) {
        update(player, state -> state.setPain(part, pain));
    }

    public static void setFractured(ServerPlayer player, BodyPart part, boolean fractured) {
        update(player, state -> state.setFractured(part, fractured));
    }

    public static void setBlackened(ServerPlayer player, BodyPart part, boolean blackened) {
        update(player, state -> state.setBlackened(part, blackened));
    }

    public static TreatmentResult applyRepair(ServerPlayer player) {
        TreatmentResult[] result = new TreatmentResult[1];
        update(player, state -> result[0] = state.applyRepair());
        return result[0];
    }

    public static TreatmentResult applyRegenerationUnlock(ServerPlayer player) {
        TreatmentResult[] result = new TreatmentResult[1];
        update(player, state -> result[0] = state.applyRegenerationUnlock());
        return result[0];
    }

    public static void applyAnalgesia(ServerPlayer player, int durationTicks) {
        player.addEffect(new MobEffectInstance(ModEffects.ANALGESIA.get(), Math.max(1, durationTicks),
                0, false, false, false));
        sync(player);
    }

    public static boolean isAnalgesiaActive(LivingEntity entity) {
        return entity.hasEffect(ModEffects.ANALGESIA.get());
    }

    public static int fracturedArmCount(Player player) {
        return get(player).fracturedArmCount();
    }

    public static int fracturedLegCount(Player player) {
        return get(player).fracturedLegCount();
    }

    public static int blackenedArmCount(Player player) {
        return get(player).blackenedArmCount();
    }

    public static int blackenedLegCount(Player player) {
        return get(player).blackenedLegCount();
    }

    public static void clear(ServerPlayer player) {
        InjuryState before = PlayerInjuryStore.get(player);
        InjuryState after = before.copy();
        after.clearInjuries();
        commit(player, before, after, true);
    }

    public static void sync(ServerPlayer player) {
        InjuryState state = PlayerInjuryStore.get(player);
        HealthFxNetwork.send(player, InjuryStateSyncPacket.of(player.getUUID(), state,
                isAnalgesiaActive(player), player.hasEffect(ModEffects.REPAIR.get()),
                player.hasEffect(MobEffects.REGENERATION)));
    }

    public static void reconcileAuthoritativeEffects(ServerPlayer player) {
        InjuryEffectBridge.reconcileAuthoritativeEffects(player);
    }

    /** Prevents an LR blood pack's bundled regeneration from treating a second condition. */
    public static void consumeActiveRegenerationTreatment(ServerPlayer player) {
        InjuryEffectBridge.consumeActiveRegenerationTreatment(player);
    }

    /**
     * Reasserts the standalone seven-part authority after vanilla healing and
     * regeneration have ticked. Supported Tarkov 5.0.x instances retain their
     * own VanillaHealthProjection and are deliberately left untouched.
     */
    public static void reconcileStandaloneVanillaHealth(ServerPlayer player) {
        projectStandaloneVanillaHealth(player, PlayerInjuryStore.get(player));
    }

    /**
     * Stores an externally timed command effect, projecting only its derived
     * conditions and retiring stale projections owned by the authority.
     */
    public static void commitFromEffect(
            ServerPlayer player,
            InjuryState before,
            InjuryState after,
            int externallyOwnedMask
    ) {
        PlayerInjuryStore.put(player, after);
        InjuryEffectBridge.projectMutationExcluding(
                player, before, after, externallyOwnedMask);
        projectStandaloneVanillaHealth(player, after);
        sync(player);
    }

    public static void update(ServerPlayer player, Consumer<InjuryState> mutation) {
        Objects.requireNonNull(mutation, "mutation");
        InjuryState before = PlayerInjuryStore.get(player);
        InjuryState after = before.copy();
        mutation.accept(after);
        if (!after.equals(before)) {
            commit(player, before, after, true);
        } else {
            sync(player);
        }
    }

    private static void commit(ServerPlayer player, InjuryState before, InjuryState after, boolean project) {
        PlayerInjuryStore.put(player, after);
        if (project) {
            InjuryEffectBridge.projectMutation(player, before, after);
        }
        projectStandaloneVanillaHealth(player, after);
        sync(player);
    }

    /**
     * The Lab owns seven-part health when run standalone. Vanilla hearts mirror
     * its aggregate ratio directly, so armor/i-frames cannot distort a projection.
     * With Tarkov loaded, its existing VanillaHealthProjection remains the owner.
     */
    private static void projectStandaloneVanillaHealth(ServerPlayer player, InjuryState state) {
        if (hasSupportedTarkovHealthAuthority()) {
            return;
        }
        double current = 0.0D;
        double maximum = 0.0D;
        for (BodyPart part : BodyPart.values()) {
            current += state.health(part);
            maximum += state.maximumHealth(part);
        }
        float before = player.getHealth();
        float projected = state.isLethal() ? 0.0F : maximum <= 0.0D
                ? player.getMaxHealth()
                : (float) (player.getMaxHealth() * Math.max(0.0D, Math.min(1.0D, current / maximum)));
        player.setHealth(projected);
        if (before > 0.0F && projected <= 0.0F) {
            player.die(player.damageSources().generic());
        }
    }

    private static boolean hasSupportedTarkovHealthAuthority() {
        return ModList.get().getModContainerById("tarkovmod")
                .map(container -> container.getModInfo().getVersion().toString())
                .map(String::trim)
                .filter(version -> version.equals("5.0") || version.startsWith("5.0."))
                .isPresent();
    }
}
