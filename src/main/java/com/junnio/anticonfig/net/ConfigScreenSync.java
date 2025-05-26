package com.junnio.anticonfig.net;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.junnio.anticonfig.config.ModConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static net.fabricmc.fabric.impl.networking.NetworkingImpl.LOGGER;

public class ConfigScreenSync {
    public static void initServer() {
        // Register the payload type
        PayloadTypeRegistry.playC2S().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);

        // Register the receiver
        ServerPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                boolean mismatch = false;
                StringBuilder mismatched = new StringBuilder();
                Map<String, String> serverConfigs = new HashMap<>();
                ModConfig config = ModConfig.getInstance();
                Path configDir = FabricLoader.getInstance().getConfigDir();

                // Get current server configs
                for (String filename : config.getConfigFilesToCheck()) {
                    Path configPath = configDir.resolve(filename);
                    if (Files.exists(configPath)) {
                        FileConfig fileConfig;
                        if (filename.endsWith(".json")) {
                            fileConfig = FileConfig.of(configPath, JsonFormat.minimalInstance());
                        } else if (filename.endsWith(".toml")) {
                            fileConfig = FileConfig.of(configPath, TomlFormat.instance());
                        } else if(filename.endsWith(".yaml") || filename.endsWith(".yml")){
                            fileConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
                        }else {
                            LOGGER.warn("Unsupported config format for file: {}", filename);
                            continue;
                        }
                        fileConfig.load();
                        serverConfigs.put(filename, fileConfig.valueMap().toString());
                    }
                }

                // Compare with client configs
                Map<String, String> clientConfigs = payload.configs();
                for (Map.Entry<String, String> entry : serverConfigs.entrySet()) {
                    String filename = entry.getKey();
                    String serverContent = entry.getValue();
                    String clientContent = clientConfigs.get(filename);

                    if (!serverContent.equals(clientContent)) {
                        mismatch = true;
                        mismatched.append(filename).append(", ");
                    }
                }

                if (mismatch) {
                    String files = mismatched.substring(0, mismatched.length() - 2);
                    //context.player().sendMessage(Text.literal("§cConfig mismatch detected! Please make sure these configs match the server: " + files));
                    // Optionally kick the player if configs don't match
                    context.player().networkHandler.disconnect(Text.literal("Config mismatch! Please make sure these configs match the server: " + files));
                }
            });
        });
    }
}