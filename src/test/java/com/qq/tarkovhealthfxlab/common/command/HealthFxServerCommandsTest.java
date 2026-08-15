package com.qq.tarkovhealthfxlab.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class HealthFxServerCommandsTest {
    @Test
    void dedicatedRootContainsPreviouslyInterceptedCommands() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        HealthFxServerCommands.register(dispatcher);

        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("healthfx_server");
        assertNotNull(root);
        CommandNode<CommandSourceStack> analgesia = child(root, "analgesia");
        assertNotNull(child(child(analgesia, "off"), "target"));
        assertNotNull(child(child(root, "head_redirect"), "status"));
    }

    @Test
    void legacyNamespacedServerSurfaceRemainsAvailable() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        HealthFxServerCommands.register(dispatcher);

        CommandNode<CommandSourceStack> healthfx = dispatcher.getRoot().getChild("healthfx");
        assertNotNull(healthfx);
        assertNotNull(healthfx.getChild("server").getChild("set"));
    }

    private static CommandNode<CommandSourceStack> child(
            CommandNode<CommandSourceStack> parent,
            String name
    ) {
        CommandNode<CommandSourceStack> result = parent.getChild(name);
        assertNotNull(result, () -> "Missing '" + name + "' below '" + parent.getName()
                + "'; children=" + parent.getChildren().stream().map(CommandNode::getName).toList());
        return result;
    }
}
