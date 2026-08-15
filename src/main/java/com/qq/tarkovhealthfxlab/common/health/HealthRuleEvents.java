package com.qq.tarkovhealthfxlab.common.health;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import com.qq.tarkovhealthfxlab.common.command.HealthFxServerCommands;
import com.qq.tarkovhealthfxlab.common.effect.InjuryEffectBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TarkovHealthFxLab.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HealthRuleEvents {
    private static final java.util.UUID LEG_MOVEMENT_MODIFIER_ID =
            java.util.UUID.fromString("1fd2c9bc-5c88-4cd4-9ed1-5488b356bba1");
    private HealthRuleEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HealthFxServerCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!event.getEntity().level().isClientSide
                && event.getEntity() instanceof ServerPlayer player) {
            InjuryEffectBridge.noteSpecialEffectApplication(
                    player, event.getEffectInstance().getEffect());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        InjuryEffectBridge.tick(player);
        tickBleeding(player);
        reconcileMovementPenalty(player);
        // Vanilla regeneration/healing runs earlier in the entity tick. The
        // Lab's seven-part state remains the standalone authority, so restore
        // the compatibility hearts after every source has had its turn.
        HealthRuleService.reconcileStandaloneVanillaHealth(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HealthRuleService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HealthRuleService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HealthRuleService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AttributeInstance speed = event.getEntity().getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(LEG_MOVEMENT_MODIFIER_ID);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer replacement)) return;
        if (event.isWasDeath()) {
            PlayerInjuryStore.clear(replacement);
            return;
        }
        event.getOriginal().reviveCaps();
        try {
            PlayerInjuryStore.replaceRoot(replacement, PlayerInjuryStore.copyRoot(event.getOriginal()));
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    private static void tickBleeding(ServerPlayer player) {
        long tick = player.level().getGameTime();
        if (tick % 20L != 0L || player.isCreative() || player.isSpectator() || !player.isAlive()) return;
        InjuryState state = PlayerInjuryStore.get(player);
        float damage = 0.0F;
        float[] regionalDamage = new float[BodyPart.values().length];
        for (BodyPart part : BodyPart.values()) {
            if (state.bleeding(part) == BleedingSeverity.HEAVY) regionalDamage[part.ordinal()] = 0.5F;
            else if (state.bleeding(part) == BleedingSeverity.LIGHT && tick % 40L == 0L) regionalDamage[part.ordinal()] = 0.25F;
            damage += regionalDamage[part.ordinal()];
        }
        if (damage > 0.0F) {
            // Seven-part truth is debited once. Vanilla health is a compatibility projection,
            // not a second MobEffect damage source.
            HealthRuleService.update(player, current -> {
                for (BodyPart part : BodyPart.values()) {
                    if (regionalDamage[part.ordinal()] > 0.0F) current.damage(part, regionalDamage[part.ordinal()]);
                }
            });
        }
    }

    private static void reconcileMovementPenalty(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        InjuryState state = PlayerInjuryStore.get(player);
        int fractureOnly = 0;
        for (BodyPart part : new BodyPart[]{BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG}) {
            if (state.isFractured(part) && !state.isBlackened(part)) fractureOnly++;
        }
        double amount = MovementPenaltyCalculator.modifierAmount(
                fractureOnly, state.blackenedLegCount(), HealthRuleService.isAnalgesiaActive(player));
        AttributeModifier old = speed.getModifier(LEG_MOVEMENT_MODIFIER_ID);
        if (old != null && Math.abs(old.getAmount() - amount) < 0.000001D) return;
        if (old != null) speed.removeModifier(LEG_MOVEMENT_MODIFIER_ID);
        if (amount < 0.0D) {
            speed.addTransientModifier(new AttributeModifier(LEG_MOVEMENT_MODIFIER_ID,
                    "Tarkov Health FX leg injury", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
