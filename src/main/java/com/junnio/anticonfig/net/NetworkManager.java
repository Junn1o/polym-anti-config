package com.junnio.anticonfig.net;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.junnio.anticonfig.Anticonfig;
import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.net.parser.Json5Parser;
import com.junnio.anticonfig.net.parser.PropertiesParser;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NetworkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AntiConfig");
    public static final Identifier CONFIG_SYNC_ID = Identifier.of(Anticonfig.MODID, "config_sync");
    public static void init() {
        // Server sends configs to client during login
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            Map<String, String> serverConfigs = new HashMap<>();
            Path configDir = FabricLoader.getInstance().getConfigDir();

            // Load server configs based on ModConfig
            ModConfig config = ModConfig.getInstance();
            for (String filename : config.getConfigFilesToCheck()) {
                Path configPath = ModConfig.resolveConfigPath(filename);
                if (Files.exists(configPath)) {
                    FileConfig fileConfig;
                    if (filename.endsWith(".json")) {
                        fileConfig = FileConfig.of(configPath, JsonFormat.minimalInstance());
                        fileConfig.load();
                        serverConfigs.put(filename, fileConfig.valueMap().toString());
                    } else if (filename.endsWith(".toml")) {
                        fileConfig = FileConfig.of(configPath, TomlFormat.instance());
                        fileConfig.load();
                        serverConfigs.put(filename, fileConfig.valueMap().toString());
                    } else if(filename.endsWith(".yaml") || filename.endsWith(".yml")){
                        fileConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
                        fileConfig.load();
                        serverConfigs.put(filename, fileConfig.valueMap().toString());
                    }else if (filename.endsWith(".json5")) {
                        try {
                            String serverContent;
                            serverContent = Json5Parser.json5ToString(configPath);
                            System.out.println("Server content: " + serverContent);
                            serverConfigs.put(filename, serverContent);
                        } catch (Exception e) {
                            LOGGER.error("Failed to parse json5 file: " + filename, e);
                        }
                    }else if (filename.endsWith(".properties")) {
                        try {
                            String serverContent = PropertiesParser.propertiesToString(configPath);
                            serverConfigs.put(filename, serverContent);
                        } catch (Exception e) {
                            LOGGER.error("Failed to parse properties file: " + filename, e);
                            continue;
                        }
                    }
                    else {
                        LOGGER.warn("Unsupported config format for file: {}", filename);
                    }
                }
            }

            // Send server configs to client
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeMap(serverConfigs, PacketByteBuf::writeString, PacketByteBuf::writeString);
            sender.sendPacket(NetworkManager.CONFIG_SYNC_ID, buf);
        });

        // Server receives and validates client configs
        ServerLoginNetworking.registerGlobalReceiver(NetworkManager.CONFIG_SYNC_ID, (server, handler, understood, buf, synchronizer, responseSender) -> {
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
                    Path configPath = ModConfig.resolveConfigPath(filename);
                    if (Files.exists(configPath)) {
                        String serverContent;
                        if (filename.endsWith(".json")) {
                            FileConfig serverConfig = FileConfig.of(configPath, JsonFormat.minimalInstance());
                            serverConfig.load();
                            serverContent = serverConfig.valueMap().toString();
                        } else if (filename.endsWith(".toml")) {
                            FileConfig serverConfig = FileConfig.of(configPath, TomlFormat.instance());
                            serverConfig.load();
                            serverContent = serverConfig.valueMap().toString();
                        } else if(filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                            FileConfig serverConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
                            serverConfig.load();
                            serverContent = serverConfig.valueMap().toString();
                        } else if (filename.endsWith(".json5")) {
                            try {
                                serverContent = Json5Parser.json5ToString(configPath);
                            } catch (Exception e) {
                                LOGGER.error("Failed to parse json5 file: " + filename, e);
                                continue;
                            }
                        } else if (filename.endsWith(".properties")) {
                            try {
                                serverContent = PropertiesParser.propertiesToString(configPath);
                            } catch (Exception e) {
                                LOGGER.error("Failed to parse properties file: " + filename, e);
                                continue;
                            }
                        }
                        else {
                            continue;
                        }

                        if (!serverContent.equals(clientContent)) {
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