package com.junnio.anticonfig;

import com.junnio.anticonfig.config.ModConfig;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class ReloadCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("anticonfig")
                    .then(CommandManager.literal("reload")
                            .requires(source -> source.hasPermissionLevel(2)) // Requires operator permission
                            .executes(context -> {
                                ModConfig.load();
                                context.getSource().sendFeedback(() -> Text.literal("Configuration reloaded"), true);
                                return Command.SINGLE_SUCCESS;
                            })));
        });
    }
}

