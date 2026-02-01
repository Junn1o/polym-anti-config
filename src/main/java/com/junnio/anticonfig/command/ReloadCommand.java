package com.junnio.anticonfig.command;

import com.junnio.anticonfig.config.ModConfig;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public class ReloadCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("anticonfig")
                    .then(Commands.literal("reload")
                            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER)) // Requires operator permission
                            .executes(context -> {
                                ModConfig.init();
                                context.getSource().sendSuccess(() -> Component.literal("Configuration reloaded"),true);
                                return Command.SINGLE_SUCCESS;
                            })));
        });
    }
}

