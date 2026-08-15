package com.qq.tarkovhealthfxlab.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.qq.tarkovhealthfxlab.common.effect.ModEffects;
import com.qq.tarkovhealthfxlab.common.health.BleedingSeverity;
import com.qq.tarkovhealthfxlab.common.health.BodyPart;
import com.qq.tarkovhealthfxlab.common.health.DamageApplication;
import com.qq.tarkovhealthfxlab.common.health.HealthRuleService;
import com.qq.tarkovhealthfxlab.common.health.HeadDamageRedirectSavedData;
import com.qq.tarkovhealthfxlab.common.health.InjuryState;
import com.qq.tarkovhealthfxlab.common.health.TreatmentResult;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public final class HealthFxServerCommands {
    private static final DynamicCommandExceptionType BAD_PART = new DynamicCommandExceptionType(
            value -> Component.literal("Unknown body part: " + value));
    private static final DynamicCommandExceptionType BAD_BLEEDING = new DynamicCommandExceptionType(
            value -> Component.literal("Unknown bleeding level: " + value));

    private HealthFxServerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("healthfx").then(serverCommands("server")));
        // F8 uses a dedicated root. Forge's client command dispatcher also owns
        // /healthfx, and on some clients it consumed newer server-only children
        // such as analgesia/head_redirect before the packet reached the server.
        dispatcher.register(serverCommands("healthfx_server"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> serverCommands(
            String root
    ) {
        var command = Commands.literal(root).requires(source -> source.hasPermission(2));
        command.then(Commands.literal("status")
                .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
        command.then(Commands.literal("clear")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> clear(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        var set = Commands.literal("set");
        set.then(Commands.literal("part_hp")
                .then(targetPartFloat((player, part, value) ->
                        HealthRuleService.setPartHealth(player, part, value))));
        set.then(Commands.literal("max_hp")
                .then(targetPartFloat((player, part, value) ->
                        HealthRuleService.setMaximumPartHealth(player, part, value))));
        set.then(Commands.literal("pain")
                .then(targetPartFloat((player, part, value) ->
                        HealthRuleService.setPain(player, part, value))));
        set.then(Commands.literal("bleeding")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(partArgument()
                                .then(Commands.argument("severity", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Stream.of(BleedingSeverity.values())
                                                        .map(v -> v.name().toLowerCase(Locale.ROOT)),
                                                builder))
                                        .executes(HealthFxServerCommands::setBleeding)))));
        set.then(Commands.literal("fracture")
                .then(targetPartBoolean((player, part, value) ->
                        HealthRuleService.setFractured(player, requireLimb(part), value))));
        set.then(Commands.literal("blackened")
                .then(targetPartBoolean((player, part, value) ->
                        HealthRuleService.setBlackened(player, requireLimb(part), value))));
        command.then(set);

        command.then(Commands.literal("damage")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(partArgument()
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F))
                                        .executes(ctx -> damage(ctx, System.nanoTime()))
                                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                                .executes(ctx -> damage(ctx,
                                                        LongArgumentType.getLong(ctx, "seed"))))))));
        command.then(Commands.literal("repair")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(HealthFxServerCommands::repair)));
        command.then(Commands.literal("analgesia")
                .then(Commands.literal("on")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                        .executes(HealthFxServerCommands::analgesiaOn))))
                .then(Commands.literal("off")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(HealthFxServerCommands::analgesiaOff))));
        command.then(Commands.literal("regeneration")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(HealthFxServerCommands::regeneration))));
        command.then(Commands.literal("head_redirect")
                .then(Commands.literal("status").executes(HealthFxServerCommands::redirectStatus))
                .then(Commands.literal("on").executes(ctx -> redirectSet(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> redirectSet(ctx, false)))
                .then(Commands.literal("test")
                        .then(Commands.argument("damage", FloatArgumentType.floatArg(0.0F))
                                .executes(ctx -> redirectTest(ctx, System.nanoTime()))
                                .then(Commands.argument("seed", LongArgumentType.longArg())
                                        .executes(ctx -> redirectTest(ctx,
                                                LongArgumentType.getLong(ctx, "seed")))))));
        return command;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> partArgument() {
        return Commands.argument("part", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(BodyPart.values()).map(BodyPart::id), builder));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> targetPartFloat(PartFloatAction action) {
        return Commands.argument("target", EntityArgument.player())
                .then(partArgument().then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 10000.0F))
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
                            BodyPart part = part(ctx);
                            float value = FloatArgumentType.getFloat(ctx, "value");
                            action.apply(player, part, value);
                            ctx.getSource().sendSuccess(() -> Component.literal("Updated " + player.getScoreboardName()
                                    + " " + part.id() + " = " + value), false);
                            return 1;
                        })));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> targetPartBoolean(PartBooleanAction action) {
        return Commands.argument("target", EntityArgument.player())
                .then(partArgument().then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
                            BodyPart part = part(ctx);
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            action.apply(player, part, value);
                            ctx.getSource().sendSuccess(() -> Component.literal("Updated " + player.getScoreboardName()
                                    + " " + part.id() + " = " + value), false);
                            return 1;
                        })));
    }

    private static int setBleeding(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        BodyPart part = part(ctx);
        BleedingSeverity severity;
        try {
            severity = BleedingSeverity.parse(StringArgumentType.getString(ctx, "severity"));
        } catch (IllegalArgumentException exception) {
            throw BAD_BLEEDING.create(StringArgumentType.getString(ctx, "severity"));
        }
        HealthRuleService.setBleeding(player, part, severity);
        ctx.getSource().sendSuccess(() -> Component.literal("Bleeding " + part.id() + " = " + severity), false);
        return 1;
    }

    private static int damage(CommandContext<CommandSourceStack> ctx, long seed) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        DamageApplication result = HealthRuleService.damagePart(player, part(ctx),
                FloatArgumentType.getFloat(ctx, "amount"), seed);
        ctx.getSource().sendSuccess(() -> redirectText(result), false);
        return 1;
    }

    private static int repair(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        TreatmentResult result = HealthRuleService.applyRepair(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Repair: " + result.condition()
                + result.treatedPart().map(part -> " " + part.id()).orElse("")
                + ", painCleared=" + result.painCleared()), false);
        return result.changed() ? 1 : 0;
    }

    private static int analgesiaOn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        HealthRuleService.applyAnalgesia(player, IntegerArgumentType.getInteger(ctx, "seconds") * 20);
        return 1;
    }

    private static int analgesiaOff(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        player.removeEffect(ModEffects.ANALGESIA.get());
        HealthRuleService.sync(player);
        return 1;
    }

    private static int regeneration(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        int ticks = IntegerArgumentType.getInteger(ctx, "seconds") * 20;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, false, true));
        return 1;
    }

    private static int redirectStatus(CommandContext<CommandSourceStack> ctx) {
        HeadDamageRedirectSavedData data = HeadDamageRedirectSavedData.get(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("head_redirect=" + data.enabled()
                + (data.last() == null ? ", last=none" : ", last: " + redirectText(data.last()).getString())), false);
        return data.enabled() ? 1 : 0;
    }

    private static int redirectSet(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        HeadDamageRedirectSavedData data = HeadDamageRedirectSavedData.get(ctx.getSource().getServer());
        data.setEnabled(enabled);
        ctx.getSource().sendSuccess(() -> Component.literal("head_redirect=" + enabled), true);
        return 1;
    }

    private static int redirectTest(CommandContext<CommandSourceStack> ctx, long seed) {
        HeadDamageRedirectSavedData data = HeadDamageRedirectSavedData.get(ctx.getSource().getServer());
        DamageApplication result = data.resolveAndRecord(BodyPart.HEAD,
                FloatArgumentType.getFloat(ctx, "damage"), seed);
        ctx.getSource().sendSuccess(() -> redirectText(result), false);
        return 1;
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        InjuryState state = HealthRuleService.get(player);
        StringBuilder text = new StringBuilder(player.getScoreboardName()).append(" injury truth:");
        for (BodyPart part : BodyPart.values()) {
            text.append("\n").append(part.id()).append(" ")
                    .append(trim(state.health(part))).append("/").append(trim(state.maximumHealth(part)))
                    .append(" bleed=").append(state.bleeding(part).name().toLowerCase(Locale.ROOT))
                    .append(" pain=").append(trim(state.pain(part)));
            if (part.isLimb()) text.append(" fracture=").append(state.isFractured(part))
                    .append(" blackened=").append(state.isBlackened(part));
        }
        text.append("\nlastAffected=").append(state.lastAffectedPart().id())
                .append(" analgesia=").append(HealthRuleService.isAnalgesiaActive(player));
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static int clear(CommandSourceStack source, ServerPlayer player) {
        HealthRuleService.clear(player);
        source.sendSuccess(() -> Component.literal("Cleared injuries for " + player.getScoreboardName()), false);
        return 1;
    }

    private static BodyPart part(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String value = StringArgumentType.getString(ctx, "part");
        try {
            return BodyPart.parse(value);
        } catch (IllegalArgumentException exception) {
            throw BAD_PART.create(value);
        }
    }

    private static BodyPart requireLimb(BodyPart part) throws CommandSyntaxException {
        if (!part.isLimb()) throw BAD_PART.create(part.id() + " (expected arm/leg)");
        return part;
    }

    private static Component redirectText(DamageApplication result) {
        return Component.literal("seed=" + result.seed() + " original=" + result.originalPart().id()
                + " target=" + result.appliedPart().id() + " damage=" + trim(result.amount())
                + " redirected=" + result.redirected());
    }

    private static String trim(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @FunctionalInterface private interface PartFloatAction {
        void apply(ServerPlayer player, BodyPart part, float value) throws CommandSyntaxException;
    }
    @FunctionalInterface private interface PartBooleanAction {
        void apply(ServerPlayer player, BodyPart part, boolean value) throws CommandSyntaxException;
    }
}
