package com.qq.tarkovhealthfxlab.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.qq.tarkovhealthfxlab.client.BleedingLevel;
import com.qq.tarkovhealthfxlab.client.BodyRegion;
import com.qq.tarkovhealthfxlab.client.HealthFxController;
import com.qq.tarkovhealthfxlab.client.HealthFxPreset;
import com.qq.tarkovhealthfxlab.client.HealthFxSource;
import com.qq.tarkovhealthfxlab.client.screen.HealthFxLabScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class HealthFxClientCommands {
    private HealthFxClientCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("healthfx")
                .executes(context -> openUi(context.getSource()))
                .then(Commands.literal("ui").executes(context -> openUi(context.getSource())))
                .then(Commands.literal("reset").executes(context -> {
                    HealthFxController.applyPreset(HealthFxPreset.OFF);
                    return success(context.getSource(), "healthfx.command.reset");
                }))
                .then(Commands.literal("preset")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (HealthFxPreset preset : HealthFxPreset.values()) {
                                        builder.suggest(preset.name().toLowerCase());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    try {
                                        HealthFxPreset preset = HealthFxPreset.parse(
                                                StringArgumentType.getString(context, "name"));
                                        HealthFxController.applyPreset(preset);
                                        return success(context.getSource(), "healthfx.command.preset", preset.name().toLowerCase());
                                    } catch (IllegalArgumentException failure) {
                                        return failure(context.getSource(), "healthfx.command.invalid_preset");
                                    }
                                })))
                .then(Commands.literal("scene")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (HealthFxPreset preset : HealthFxPreset.values()) {
                                        builder.suggest(preset.name().toLowerCase());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> applyScene(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("source")
                        .then(Commands.literal("mock").executes(context -> {
                            HealthFxController.setSource(HealthFxSource.MOCK);
                            return success(context.getSource(), "healthfx.command.source_mock");
                        }))
                        .then(Commands.literal("lab").executes(context -> {
                            HealthFxController.setSource(HealthFxSource.LAB_SERVER);
                            return success(context.getSource(), "healthfx.command.source_lab");
                        }))
                        .then(Commands.literal("tarkov").executes(context -> {
                            HealthFxController.setSource(HealthFxSource.TARKOV_LIVE);
                            return success(context.getSource(), "healthfx.command.source_tarkov");
                        })))
                .then(Commands.literal("part")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (BodyRegion region : BodyRegion.values()) builder.suggest(region.id());
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    try {
                                        BodyRegion wanted = BodyRegion.parse(StringArgumentType.getString(context, "name"));
                                        HealthFxController.setSource(HealthFxSource.MOCK);
                                        while (HealthFxController.selectedRegion() != wanted) {
                                            HealthFxController.cycleSelectedRegion();
                                        }
                                        return success(context.getSource(), "healthfx.command.part", wanted.id());
                                    } catch (IllegalArgumentException failure) {
                                        return failure(context.getSource(), "healthfx.command.invalid_part");
                                    }
                                })))
                .then(Commands.literal("bleed")
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("none").suggest("light").suggest("heavy");
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    try {
                                        HealthFxController.setSource(HealthFxSource.MOCK);
                                        HealthFxController.setBleeding(BleedingLevel.parse(
                                                StringArgumentType.getString(context, "level")));
                                        return success(context.getSource(), "healthfx.command.updated");
                                    } catch (IllegalArgumentException failure) {
                                        return failure(context.getSource(), "healthfx.command.invalid_bleed");
                                    }
                                })))
                .then(Commands.literal("fracture")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setFracture(BoolArgumentType.getBool(context, "enabled"));
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(Commands.literal("blackened")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setBlackened(BoolArgumentType.getBool(context, "enabled"));
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(Commands.literal("pain")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setPain(DoubleArgumentType.getDouble(context, "amount"));
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(Commands.literal("health")
                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setHealthRatio(
                                            DoubleArgumentType.getDouble(context, "percent") / 100.0D);
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(Commands.literal("hp")
                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setSelectedPartHealthRatio(
                                            DoubleArgumentType.getDouble(context, "percent") / 100.0D);
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(Commands.literal("painkiller")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    boolean wanted = BoolArgumentType.getBool(context, "enabled");
                                    if (HealthFxController.mockState().painkillerActive() != wanted) {
                                        HealthFxController.togglePainkiller();
                                    }
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(clientCommands()));
    }

    /** Namespaced v2 surface; the direct v1 subcommands above remain as aliases. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> clientCommands() {
        return Commands.literal("client")
                .executes(context -> openUi(context.getSource()))
                .then(Commands.literal("ui").executes(context -> openUi(context.getSource())))
                .then(Commands.literal("reset").executes(context -> {
                    HealthFxController.applyPreset(HealthFxPreset.OFF);
                    return success(context.getSource(), "healthfx.command.reset");
                }))
                .then(Commands.literal("preset")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (HealthFxPreset preset : HealthFxPreset.values()) {
                                        builder.suggest(preset.name().toLowerCase());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> applyPreset(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("scene")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (HealthFxPreset preset : HealthFxPreset.values()) {
                                        builder.suggest(preset.name().toLowerCase());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> applyScene(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("source")
                        .then(Commands.literal("mock").executes(context -> setSource(
                                context.getSource(), HealthFxSource.MOCK, "healthfx.command.source_mock")))
                        .then(Commands.literal("lab").executes(context -> setSource(
                                context.getSource(), HealthFxSource.LAB_SERVER, "healthfx.command.source_lab")))
                        .then(Commands.literal("tarkov").executes(context -> setSource(
                                context.getSource(), HealthFxSource.TARKOV_LIVE, "healthfx.command.source_tarkov"))))
                .then(Commands.literal("part")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (BodyRegion region : BodyRegion.values()) builder.suggest(region.id());
                                    return builder.buildFuture();
                                })
                                .executes(context -> selectPart(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("bleed")
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((context, builder) -> builder.suggest("none")
                                        .suggest("light").suggest("heavy").buildFuture())
                                .executes(context -> setMockBleeding(context.getSource(),
                                        StringArgumentType.getString(context, "level")))))
                .then(Commands.literal("fracture")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setFracture(BoolArgumentType.getBool(context, "enabled"));
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(Commands.literal("blackened")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setBlackened(BoolArgumentType.getBool(context, "enabled"));
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(Commands.literal("pain")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setPain(DoubleArgumentType.getDouble(context, "amount"));
                                    return success(context.getSource(), "healthfx.command.updated");
                                })))
                .then(Commands.literal("hp")
                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                .executes(context -> {
                                    HealthFxController.setSource(HealthFxSource.MOCK);
                                    HealthFxController.setSelectedPartHealthRatio(
                                            DoubleArgumentType.getDouble(context, "percent") / 100.0D);
                                    return success(context.getSource(), "healthfx.command.updated");
                                })));
    }

    private static int applyPreset(CommandSourceStack source, String name) {
        try {
            HealthFxPreset preset = HealthFxPreset.parse(name);
            HealthFxController.applyPreset(preset);
            return success(source, "healthfx.command.preset", preset.name().toLowerCase());
        } catch (IllegalArgumentException failure) {
            return failure(source, "healthfx.command.invalid_preset");
        }
    }

    private static int applyScene(CommandSourceStack source, String name) {
        try {
            HealthFxPreset preset = HealthFxPreset.parse(name);
            if (!HealthFxController.applyScene(preset)) {
                return failure(source, "healthfx.command.scene_unavailable");
            }
            return success(source, "healthfx.command.scene", preset.name().toLowerCase());
        } catch (IllegalArgumentException failure) {
            return failure(source, "healthfx.command.invalid_preset");
        }
    }

    private static int setSource(CommandSourceStack source, HealthFxSource wanted, String messageKey) {
        HealthFxController.setSource(wanted);
        return success(source, messageKey);
    }

    private static int selectPart(CommandSourceStack source, String name) {
        try {
            BodyRegion wanted = BodyRegion.parse(name);
            HealthFxController.setSelectedRegion(wanted);
            return success(source, "healthfx.command.part", wanted.id());
        } catch (IllegalArgumentException failure) {
            return failure(source, "healthfx.command.invalid_part");
        }
    }

    private static int setMockBleeding(CommandSourceStack source, String level) {
        try {
            HealthFxController.setSource(HealthFxSource.MOCK);
            HealthFxController.setBleeding(BleedingLevel.parse(level));
            return success(source, "healthfx.command.updated");
        } catch (IllegalArgumentException failure) {
            return failure(source, "healthfx.command.invalid_bleed");
        }
    }

    private static int openUi(CommandSourceStack source) {
        Minecraft.getInstance().setScreen(new HealthFxLabScreen());
        return success(source, "healthfx.command.opened");
    }

    private static int success(CommandSourceStack source, String key, Object... arguments) {
        source.sendSystemMessage(Component.translatable(key, arguments));
        return 1;
    }

    private static int failure(CommandSourceStack source, String key) {
        source.sendFailure(Component.translatable(key));
        return 0;
    }
}
