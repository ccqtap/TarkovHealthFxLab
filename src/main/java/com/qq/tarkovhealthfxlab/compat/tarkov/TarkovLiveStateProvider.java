package com.qq.tarkovhealthfxlab.compat.tarkov;

import com.qq.tarkovhealthfxlab.client.BleedingLevel;
import com.qq.tarkovhealthfxlab.client.BodyEffect;
import com.qq.tarkovhealthfxlab.client.BodyRegion;
import com.qq.tarkovhealthfxlab.client.HealthFxState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Reflection-isolated bridge to TarkovMod 5.0's synced client health image. */
public final class TarkovLiveStateProvider {
    private static Bridge bridge;
    private static boolean bridgeAttempted;

    private TarkovLiveStateProvider() {
    }

    public static Result sample(LocalPlayer player) {
        if (player == null) {
            return Result.unavailable("healthfx.live.no_player");
        }
        if (!ModList.get().isLoaded("tarkovmod")) {
            return Result.unavailable("healthfx.live.mod_missing");
        }
        Bridge resolved = bridge();
        if (resolved == null) {
            return Result.unavailable("healthfx.live.bridge_incompatible");
        }
        try {
            if (!(Boolean) resolved.hasSnapshot.invoke(null)) {
                return Result.unavailable("healthfx.live.waiting_snapshot");
            }
            @SuppressWarnings({"rawtypes", "unchecked"})
            LazyOptional<?> optional = player.getCapability((Capability) resolved.capability);
            Object provider = optional.resolve().orElse(null);
            if (provider == null) {
                return Result.unavailable("healthfx.live.capability_missing");
            }
            Object state = resolved.providerState.invoke(provider);
            @SuppressWarnings("unchecked")
            Map<Object, Double> current = (Map<Object, Double>) resolved.currentValues.invoke(state);
            @SuppressWarnings("unchecked")
            Map<Object, Double> maximum = (Map<Object, Double>) resolved.maximumValues.invoke(state);
            Object conditions = resolved.conditions.invoke(state);

            EnumMap<BodyRegion, BodyEffect> effects = new EnumMap<>(BodyRegion.class);
            double currentTotal = 0.0D;
            double maximumTotal = 0.0D;
            for (Map.Entry<Object, Double> entry : current.entrySet()) {
                BodyRegion region = BodyRegion.valueOf(entry.getKey().toString());
                Object condition = resolved.partCondition.invoke(conditions, entry.getKey());
                Object bleeding = resolved.bleeding.invoke(condition);
                boolean fractured = (Boolean) resolved.fractured.invoke(condition);
                double pain = ((Number) resolved.pain.invoke(condition)).doubleValue();
                double maximumHealth = maximum.getOrDefault(entry.getKey(), region.defaultMaximumHealth());
                if (!Double.isFinite(maximumHealth) || maximumHealth <= 0.0D) {
                    maximumHealth = region.defaultMaximumHealth();
                }
                double currentHealth = Math.max(0.0D, Math.min(maximumHealth, entry.getValue()));
                boolean blackened = (region.isArm() || region.isLeg()) && currentHealth == 0.0D;
                effects.put(region, new BodyEffect(
                        BleedingLevel.valueOf(bleeding.toString()), fractured, blackened, pain,
                        currentHealth, maximumHealth));
                currentTotal += currentHealth;
                maximumTotal += maximumHealth;
            }
            long gameTick = player.level().getGameTime();
            boolean painkiller = (Boolean) resolved.painkillerActive.invoke(conditions, gameTick);
            long revision = ((Number) resolved.revision.invoke(state)).longValue();
            double ratio = maximumTotal <= 0.0D ? 1.0D : currentTotal / maximumTotal;
            return Result.available(HealthFxState.restore(
                    effects, Math.max(0.0D, Math.min(1.0D, ratio)), painkiller, revision));
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return Result.unavailable("healthfx.live.read_failed");
        }
    }

    private static synchronized Bridge bridge() {
        if (bridgeAttempted) {
            return bridge;
        }
        bridgeAttempted = true;
        try {
            Class<?> handlers = Class.forName("com.qq.tarkovmod.client.ClientNetworkPacketHandlers");
            Class<?> capabilities = Class.forName("com.qq.tarkovmod.capability.ModCapabilities");
            Class<?> provider = Class.forName("com.qq.tarkovmod.health.capability.TarkovHealthProvider");
            Class<?> state = Class.forName("com.qq.tarkovmod.health.model.HealthState");
            Class<?> conditions = Class.forName("com.qq.tarkovmod.health.condition.HealthConditions");
            Class<?> bodyPart = Class.forName("com.qq.tarkovmod.health.model.BodyPart");
            Class<?> partCondition = Class.forName("com.qq.tarkovmod.health.condition.PartCondition");
            Field capabilityField = capabilities.getField("TARKOV_HEALTH_CAPABILITY");
            bridge = new Bridge(
                    handlers.getMethod("hasAuthoritativeHealthSnapshot"),
                    capabilityField.get(null),
                    provider.getMethod("state"),
                    state.getMethod("currentValues"),
                    state.getMethod("maximumValues"),
                    state.getMethod("conditions"),
                    state.getMethod("revision"),
                    conditions.getMethod("part", bodyPart),
                    conditions.getMethod("painkillerActive", long.class),
                    partCondition.getMethod("bleeding"),
                    partCondition.getMethod("fractured"),
                    partCondition.getMethod("pain")
            );
        } catch (ReflectiveOperationException | LinkageError failure) {
            bridge = null;
        }
        return bridge;
    }

    private record Bridge(
            Method hasSnapshot,
            Object capability,
            Method providerState,
            Method currentValues,
            Method maximumValues,
            Method conditions,
            Method revision,
            Method partCondition,
            Method painkillerActive,
            Method bleeding,
            Method fractured,
            Method pain
    ) {
    }

    public record Result(Optional<HealthFxState> state, String statusKey) {
        private static Result available(HealthFxState state) {
            return new Result(Optional.of(state), "healthfx.live.connected");
        }

        private static Result unavailable(String statusKey) {
            return new Result(Optional.empty(), statusKey);
        }
    }
}
