package com.junnio.antifreecam.net;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.junnio.antifreecam.config.ConfigSync;
import com.junnio.antifreecam.config.ModConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NetworkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AntiFreecam");

    public static void init() {
        // Server sends configs to client during login
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            Map<String, String> serverConfigs = new HashMap<>();
            Path configDir = FabricLoader.getInstance().getConfigDir();

            // Load server configs based on ModConfig
            ModConfig config = ModConfig.getInstance();
            for (String filename : config.getConfigFilesToCheck()) {
                Path configPath = configDir.resolve(filename);
                if (Files.exists(configPath)) {
                    FileConfig fileConfig;
                    if (filename.endsWith(".json")) {
                        fileConfig = FileConfig.of(configPath, JsonFormat.minimalInstance());
                    } else if (filename.endsWith(".toml")) {
                        fileConfig = FileConfig.of(configPath, TomlFormat.instance());
                    } else {
                        LOGGER.warn("Unsupported config format for file: {}", filename);
                        continue;
                    }
                    fileConfig.load();
                    serverConfigs.put(filename, fileConfig.valueMap().toString());
                }
            }

            // Send server configs to client
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeMap(serverConfigs, PacketByteBuf::writeString, PacketByteBuf::writeString);
            sender.sendPacket(ConfigSync.CONFIG_SYNC_ID, buf);
        });

        // Server receives and validates client configs
        ServerLoginNetworking.registerGlobalReceiver(ConfigSync.CONFIG_SYNC_ID, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!understood) {
                LOGGER.warn("Client doesn't have the mod installed");
                return;
            }

            // Read client configs
            Map<String, String> clientConfigs = buf.readMap(PacketByteBuf::readString, PacketByteBuf::readString);

            // Check each config file from ModConfig
            boolean mismatch = false;
            StringBuilder mismatched = new StringBuilder();

            ModConfig config = ModConfig.getInstance();
            Path configDir = FabricLoader.getInstance().getConfigDir();

            for (String filename : config.getConfigFilesToCheck()) {
                if (clientConfigs.containsKey(filename)) {
                    String clientContent = clientConfigs.get(filename);
                    Path configPath = configDir.resolve(filename);

                    if (Files.exists(configPath)) {
                        FileConfig serverConfig;
                        if (filename.endsWith(".json")) {
                            serverConfig = FileConfig.of(configPath, JsonFormat.minimalInstance());
                        } else if (filename.endsWith(".toml")) {
                            serverConfig = FileConfig.of(configPath, TomlFormat.instance());
                        } else {
                            continue;
                        }
                        serverConfig.load();

                        if (!serverConfig.valueMap().toString().equals(clientContent)) {
                            mismatch = true;
                            mismatched.append(filename).append(", ");
                        }
                    }
                }
            }

            if (mismatch) {
                String files = mismatched.substring(0, mismatched.length() - 2);
                handler.disconnect(Text.literal("Config mismatch! Please make sure these configs match the server: " + files));
            }
        });
    }
}