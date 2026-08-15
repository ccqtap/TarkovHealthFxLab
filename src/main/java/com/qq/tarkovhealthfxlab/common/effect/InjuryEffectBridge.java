package com.qq.tarkovhealthfxlab.common.effect;

import com.qq.tarkovhealthfxlab.common.health.BleedingSeverity;
import com.qq.tarkovhealthfxlab.common.health.BodyPart;
import com.qq.tarkovhealthfxlab.common.health.HealthRuleService;
import com.qq.tarkovhealthfxlab.common.health.InjuryState;
import com.qq.tarkovhealthfxlab.common.health.PlayerInjuryStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Predicate;

/**
 * Bidirectional command bridge. Persistent hidden instances project service state;
 * externally timed /effect instances write to the truth only for their lifetime.
 */
public final class InjuryEffectBridge {
    private InjuryEffectBridge() {
    }

    public static void projectMutation(ServerPlayer player, InjuryState before, InjuryState after) {
        projectMutationExcluding(player, before, after, 0);
    }

    /** Projects derived/authoritative changes while leaving timed source effects externally owned. */
    public static void projectMutationExcluding(
            ServerPlayer player,
            InjuryState before,
            InjuryState after,
            int externallyOwnedMask
    ) {
        int projected = PlayerInjuryStore.getMarker(player, PlayerInjuryStore.PROJECTED_MASK_KEY);
        for (Key key : Key.values()) {
            boolean owned = (projected & key.bit) != 0;
            boolean desiredAfter = key.desired(after);
            boolean presentationChanged = !key.sameProjection(before, after);
            boolean externalSource = (externallyOwnedMask & key.bit) != 0;
            if (ProjectionPresencePolicy.shouldRemoveEffect(
                    owned, presentationChanged, desiredAfter, externalSource)) {
                projected &= ~key.bit;
                player.removeEffect(key.effect());
            } else if (owned) {
                if (presentationChanged) {
                    install(player, key, after);
                }
            } else if (desiredAfter && presentationChanged
                    && !externalSource) {
                projected |= key.bit;
                install(player, key, after);
            }
        }
        PlayerInjuryStore.setMarker(player, PlayerInjuryStore.PROJECTED_MASK_KEY, projected);
        PlayerInjuryStore.setMarker(player, PlayerInjuryStore.OBSERVED_MASK_KEY, presenceMask(player));
    }

    public static void tick(ServerPlayer player) {
        InjuryState state = PlayerInjuryStore.get(player);
        InjuryState changed = state.copy();
        int projected = PlayerInjuryStore.getMarker(player, PlayerInjuryStore.PROJECTED_MASK_KEY);
        int observed = PlayerInjuryStore.getMarker(player, PlayerInjuryStore.OBSERVED_MASK_KEY);
        boolean stateChanged = false;
        int externallyOwnedMask = 0;
        boolean externalBleedingCleared = false;

        for (Key key : Key.values()) {
            boolean active = player.hasEffect(key.effect());
            boolean wasActive = (observed & key.bit) != 0;
            boolean isProjection = (projected & key.bit) != 0;
            switch (ProjectionPresencePolicy.decide(active, wasActive, isProjection)) {
                case APPLY_EXTERNAL -> {
                    key.applyExternal(changed, player.getEffect(key.effect()));
                    externallyOwnedMask |= key.bit;
                    stateChanged = true;
                }
                case CLEAR_EXTERNAL -> {
                    key.clear(changed);
                    externallyOwnedMask |= key.bit;
                    externalBleedingCleared |= key == Key.LIGHT || key == Key.HEAVY;
                    stateChanged = true;
                }
                case NONE, RESTORE_PROJECTION -> {
                    // refreshProjected handles an absent authoritative projection.
                }
            }
        }

        if (stateChanged) {
            // Preserve any other still-active external layer. In particular,
            // LIGHT must survive a temporary HEAVY overlay and become truth
            // again when HEAVY expires.
            for (Key key : Key.values()) {
                if ((projected & key.bit) == 0 && player.hasEffect(key.effect())) {
                    externallyOwnedMask |= key.bit;
                }
            }
        }
        if (externalBleedingCleared) {
            for (Key key : new Key[]{Key.LIGHT, Key.HEAVY}) {
                if ((projected & key.bit) == 0 && player.hasEffect(key.effect())) {
                    key.applyExternal(changed, player.getEffect(key.effect()));
                }
            }
        }

        int special = PlayerInjuryStore.getMarker(player, PlayerInjuryStore.SPECIAL_OBSERVED_MASK_KEY);
        boolean repair = player.hasEffect(ModEffects.REPAIR.get());
        boolean regeneration = player.hasEffect(MobEffects.REGENERATION);
        boolean analgesia = player.hasEffect(ModEffects.ANALGESIA.get());
        int specialNow = SpecialEffectTracker.carryActive(
                special, repair, regeneration, analgesia);
        boolean repairUsed = repair && SpecialEffectTracker.repairUsed(specialNow);
        boolean regenerationUsed = regeneration
                && SpecialEffectTracker.regenerationUsed(specialNow);

        if (repair && !repairUsed) {
            if (changed.applyRepair().changed()) {
                specialNow = SpecialEffectTracker.markRepairUsed(specialNow);
                stateChanged = true;
            }
        }
        if (regeneration && !regenerationUsed) {
            if (changed.applyRegenerationUnlock().changed()) {
                specialNow = SpecialEffectTracker.markRegenerationUsed(specialNow);
                stateChanged = true;
            }
        }

        PlayerInjuryStore.setMarker(player, PlayerInjuryStore.SPECIAL_OBSERVED_MASK_KEY, specialNow);
        if (stateChanged && !changed.equals(state)) {
            HealthRuleService.commitFromEffect(player, state, changed, externallyOwnedMask);
        } else if (special != specialNow) {
            HealthRuleService.sync(player);
        }

        projected = PlayerInjuryStore.getMarker(player, PlayerInjuryStore.PROJECTED_MASK_KEY);
        projected = refreshProjected(player, changed, projected);
        PlayerInjuryStore.setMarker(player, PlayerInjuryStore.PROJECTED_MASK_KEY, projected);
        PlayerInjuryStore.setMarker(player, PlayerInjuryStore.OBSERVED_MASK_KEY, presenceMask(player));
    }

    /** Reinstalls projections from truth after broad third-party effect removal. */
    public static void reconcileAuthoritativeEffects(ServerPlayer player) {
        InjuryState state = PlayerInjuryStore.get(player);
        int previousProjected = PlayerInjuryStore.getMarker(
                player, PlayerInjuryStore.PROJECTED_MASK_KEY);
        int desired = 0;
        for (Key key : Key.values()) {
            if (key.desired(state)) desired |= key.bit;
        }
        int projected = ProjectionPresencePolicy.retainOwnedProjectionMask(
                previousProjected, desired);
        for (Key key : Key.values()) {
            if ((projected & key.bit) != 0) {
                install(player, key, state);
            } else if ((previousProjected & key.bit) != 0) {
                player.removeEffect(key.effect());
            }
        }
        PlayerInjuryStore.setMarker(player, PlayerInjuryStore.PROJECTED_MASK_KEY, projected);
        PlayerInjuryStore.setMarker(player, PlayerInjuryStore.OBSERVED_MASK_KEY, presenceMask(player));
        HealthRuleService.sync(player);
    }

    /**
     * Consumes the already-active vanilla regeneration edge when an LR blood
     * pack has just performed this treatment through its completion event.
     * Other regeneration instances keep their normal one-unlock behavior.
     */
    public static void consumeActiveRegenerationTreatment(ServerPlayer player) {
        if (!player.hasEffect(MobEffects.REGENERATION)) {
            return;
        }
        int previous = PlayerInjuryStore.getMarker(
                player, PlayerInjuryStore.SPECIAL_OBSERVED_MASK_KEY);
        int current = SpecialEffectTracker.carryActive(
                previous,
                player.hasEffect(ModEffects.REPAIR.get()),
                true,
                player.hasEffect(ModEffects.ANALGESIA.get())
        );
        PlayerInjuryStore.setMarker(
                player,
                PlayerInjuryStore.SPECIAL_OBSERVED_MASK_KEY,
                SpecialEffectTracker.markRegenerationUsed(current)
        );
    }

    /** Rearms a refreshed treatment instance even when the previous one is still active. */
    public static void noteSpecialEffectApplication(ServerPlayer player, MobEffect effect) {
        int marker = PlayerInjuryStore.getMarker(
                player, PlayerInjuryStore.SPECIAL_OBSERVED_MASK_KEY);
        if (effect == ModEffects.REPAIR.get()) {
            marker = SpecialEffectTracker.rearmRepair(marker);
        } else if (effect == MobEffects.REGENERATION) {
            marker = SpecialEffectTracker.rearmRegeneration(marker);
        } else {
            return;
        }
        PlayerInjuryStore.setMarker(
                player, PlayerInjuryStore.SPECIAL_OBSERVED_MASK_KEY, marker);
    }

    private static int refreshProjected(ServerPlayer player, InjuryState state, int projected) {
        for (Key key : Key.values()) {
            if ((projected & key.bit) == 0) continue;
            if (!key.desired(state)) {
                projected &= ~key.bit;
                player.removeEffect(key.effect());
                continue;
            }
            MobEffectInstance current = player.getEffect(key.effect());
            int amplifier = key.amplifier(state);
            if (current == null || current.getAmplifier() != amplifier || !current.isInfiniteDuration()) {
                install(player, key, state);
            }
        }
        return projected;
    }

    private static void install(ServerPlayer player, Key key, InjuryState state) {
        player.addEffect(new MobEffectInstance(key.effect(), -1, key.amplifier(state), false, false, false));
    }

    private static int presenceMask(ServerPlayer player) {
        int result = 0;
        for (Key key : Key.values()) {
            if (player.hasEffect(key.effect())) result |= key.bit;
        }
        return result;
    }

    private enum Key {
        LIGHT(0, ModEffects.LIGHT_BLEEDING, s -> s.bleeding() == BleedingSeverity.LIGHT) {
            @Override void applyExternal(InjuryState s, MobEffectInstance i) {
                s.setBleeding(s.lastAffectedPart(), BleedingSeverity.LIGHT);
            }
            @Override void clear(InjuryState s) {
                for (BodyPart p : BodyPart.values()) if (s.bleeding(p) == BleedingSeverity.LIGHT) s.setBleeding(p, BleedingSeverity.NONE);
            }
            @Override boolean sameProjection(InjuryState a, InjuryState b) {
                for (BodyPart p : BodyPart.values()) if (a.bleeding(p) != b.bleeding(p)) return false;
                return true;
            }
        },
        HEAVY(1, ModEffects.HEAVY_BLEEDING, s -> s.bleeding() == BleedingSeverity.HEAVY) {
            @Override void applyExternal(InjuryState s, MobEffectInstance i) {
                s.setBleeding(s.lastAffectedPart(), BleedingSeverity.HEAVY);
            }
            @Override void clear(InjuryState s) {
                for (BodyPart p : BodyPart.values()) if (s.bleeding(p) == BleedingSeverity.HEAVY) s.setBleeding(p, BleedingSeverity.NONE);
            }
            @Override boolean sameProjection(InjuryState a, InjuryState b) {
                for (BodyPart p : BodyPart.values()) if (a.bleeding(p) != b.bleeding(p)) return false;
                return true;
            }
        },
        PAIN(2, ModEffects.PAIN, s -> s.pain() > 0.0F) {
            @Override void applyExternal(InjuryState s, MobEffectInstance i) {
                s.setPain(s.lastAffectedPart(), Math.min(100.0F, 25.0F * (i.getAmplifier() + 1)));
            }
            @Override void clear(InjuryState s) {
                for (BodyPart p : BodyPart.values()) s.setPain(p, 0.0F);
            }
            @Override int amplifier(InjuryState s) {
                return Math.max(0, Math.min(3, (int) Math.ceil(s.pain() / 25.0F) - 1));
            }
            @Override boolean sameProjection(InjuryState a, InjuryState b) {
                for (BodyPart p : BodyPart.values()) if (Float.compare(a.pain(p), b.pain(p)) != 0) return false;
                return true;
            }
        },
        FRACTURE(3, ModEffects.FRACTURE, s -> !s.fractures().isEmpty()) {
            private final BodyPart[] limbs = {BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};
            @Override void applyExternal(InjuryState s, MobEffectInstance i) {
                s.setFractured(this.limbs[Math.floorMod(i.getAmplifier(), this.limbs.length)], true);
            }
            @Override void clear(InjuryState s) {
                for (BodyPart p : this.limbs) s.setFractured(p, false);
            }
            @Override boolean sameProjection(InjuryState a, InjuryState b) {
                return a.fractures().equals(b.fractures());
            }
        },
        BLACK_LEFT_ARM(4, ModEffects.BLACKENED_LEFT_ARM, s -> s.isBlackened(BodyPart.LEFT_ARM)) {
            @Override void applyExternal(InjuryState s, MobEffectInstance i) { s.setBlackened(BodyPart.LEFT_ARM, true); }
            @Override void clear(InjuryState s) { s.setBlackened(BodyPart.LEFT_ARM, false); }
        },
        BLACK_RIGHT_ARM(5, ModEffects.BLACKENED_RIGHT_ARM, s -> s.isBlackened(BodyPart.RIGHT_ARM)) {
            @Override void applyExternal(InjuryState s, MobEffectInstance i) { s.setBlackened(BodyPart.RIGHT_ARM, true); }
            @Override void clear(InjuryState s) { s.setBlackened(BodyPart.RIGHT_ARM, false); }
        },
        BLACK_LEFT_LEG(6, ModEffects.BLACKENED_LEFT_LEG, s -> s.isBlackened(BodyPart.LEFT_LEG)) {
            @Override void applyExternal(InjuryState s, MobEffectInstance i) { s.setBlackened(BodyPart.LEFT_LEG, true); }
            @Override void clear(InjuryState s) { s.setBlackened(BodyPart.LEFT_LEG, false); }
        },
        BLACK_RIGHT_LEG(7, ModEffects.BLACKENED_RIGHT_LEG, s -> s.isBlackened(BodyPart.RIGHT_LEG)) {
            @Override void applyExternal(InjuryState s, MobEffectInstance i) { s.setBlackened(BodyPart.RIGHT_LEG, true); }
            @Override void clear(InjuryState s) { s.setBlackened(BodyPart.RIGHT_LEG, false); }
        };

        final int bit;
        final RegistryObject<MobEffect> registry;
        final Predicate<InjuryState> predicate;

        Key(int index, RegistryObject<MobEffect> registry, Predicate<InjuryState> predicate) {
            this.bit = 1 << index;
            this.registry = registry;
            this.predicate = predicate;
        }

        MobEffect effect() { return this.registry.get(); }
        boolean desired(InjuryState state) { return this.predicate.test(state); }
        int amplifier(InjuryState state) { return 0; }
        boolean sameProjection(InjuryState a, InjuryState b) {
            return desired(a) == desired(b) && (!desired(a) || amplifier(a) == amplifier(b));
        }
        abstract void applyExternal(InjuryState state, MobEffectInstance instance);
        abstract void clear(InjuryState state);
    }
}
