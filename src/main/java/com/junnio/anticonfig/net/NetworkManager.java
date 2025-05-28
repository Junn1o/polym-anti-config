package com.junnio.anticonfig.net;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.hocon.HoconFormat;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.junnio.anticonfig.Anticonfig;
import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.net.parser.*;
import net.fabricmc.fabric.api.networking.v1.*;
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

import static net.fabricmc.fabric.impl.networking.NetworkingImpl.LOGGER;

public class NetworkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AntiConfig");
    public static final Identifier CONFIG_SYNC_ID = Identifier.of(Anticonfig.MODID, "config_sync");

    public static void init() {
        PayloadTypeRegistry.playC2S().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
        // Server sends configs to client during login
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            Map<String, String> serverConfigs = new HashMap<>();
            Path configDir = FabricLoader.getInstance().getConfigDir();

            // Load server configs based on ModConfig
            ModConfig config = ModConfig.getInstance();
            for (String filename : config.getConfigFilesToCheck()) {
                Path configPath = ModConfig.resolveConfigPath(filename);
                if (Files.exists(configPath)) {
                    String serverContent;
                    try {
                        if (filename.endsWith(".json")) {
                            FileConfig fileConfig = FileConfig.of(configPath, JsonFormat.minimalInstance());
                            fileConfig.load();
                            serverContent = NightConfigParser.configToString(fileConfig);
                        } else if (filename.endsWith(".toml")) {
                            FileConfig fileConfig = FileConfig.of(configPath, TomlFormat.instance());
                            fileConfig.load();
                            serverContent = NightConfigParser.configToString(fileConfig);
                        } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                            FileConfig fileConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
                            fileConfig.load();
                            serverContent = NightConfigParser.configToString(fileConfig);
                        } else if (filename.endsWith(".json5")) {
                            serverContent = Json5Parser.json5ToString(configPath);
                        } else if (filename.endsWith(".hocon")) {
                            FileConfig fileConfig = FileConfig.of(configPath, HoconFormat.instance());
                            fileConfig.load();
                            serverContent = NightConfigParser.configToString(fileConfig);
                        } else if (filename.endsWith(".ini")) {
                            serverContent = IniParser.iniToString(configPath);
                        } else if (filename.endsWith(".properties") || filename.endsWith(".conf") || filename.endsWith(".cfg")) {
                            serverContent = PropertiesParser.propertiesToString(configPath);
                        } else if (filename.endsWith(".txt")) {
                            serverContent = TxtConfigParser.txtToString(configPath);
                        } else {
                            continue;
                        }
                        System.out.println("Server config: " + filename + " = " + serverContent);
                        serverConfigs.put(filename, serverContent);
                    } catch (Exception e) {
                        LOGGER.error("Failed to read server config: " + filename, e);
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
                        try {
                            if (filename.endsWith(".json")) {
                                FileConfig serverConfig = FileConfig.of(configPath, JsonFormat.fancyInstance());
                                serverConfig.load();
                                serverContent = NightConfigParser.configToString(serverConfig);
                            } else if (filename.endsWith(".toml")) {
                                FileConfig serverConfig = FileConfig.of(configPath, TomlFormat.instance());
                                serverConfig.load();
                                serverContent = NightConfigParser.configToString(serverConfig);
                            } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                                FileConfig serverConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
                                serverConfig.load();
                                serverContent = NightConfigParser.configToString(serverConfig);
                            } else if (filename.endsWith(".json5")) {
                                serverContent = Json5Parser.json5ToString(configPath);
                            } else if (filename.endsWith(".hocon")) {
                                FileConfig fileConfig = FileConfig.of(configPath, HoconFormat.instance());
                                fileConfig.load();
                                serverContent = NightConfigParser.configToString(fileConfig);
                            } else if (filename.endsWith(".ini")) {
                                serverContent = IniParser.iniToString(configPath);
                            } else if (filename.endsWith(".properties") || filename.endsWith(".conf") || filename.endsWith(".cfg")) {
                                serverContent = PropertiesParser.propertiesToString(configPath);
                            } else if (filename.endsWith(".txt")) {
                                serverContent = TxtConfigParser.txtToString(configPath);
                            } else {
                                continue;
                            }
                            System.out.println("Client validate: " + filename + " = " + clientContent);
                            System.out.println("Server validate: " + filename + " = " + serverContent);
                        } catch (Exception e) {
                            LOGGER.error("Failed to read server config: " + filename, e);
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

        ServerPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                boolean mismatch = false;
                StringBuilder mismatched = new StringBuilder();
                Map<String, String> serverConfigs = new HashMap<>();
                ModConfig config = ModConfig.getInstance();

                // Get current server configs
                for (String filename : config.getConfigFilesToCheck()) {
                    Path configPath = ModConfig.resolveConfigPath(filename);
                    if (Files.exists(configPath)) {
                        try {
                            String serverContent;
                            if (filename.endsWith(".json")) {
                                FileConfig fileConfig = FileConfig.of(configPath, JsonFormat.minimalInstance());
                                fileConfig.load();
                                serverContent = NightConfigParser.configToString(fileConfig);
                            } else if (filename.endsWith(".toml")) {
                                FileConfig fileConfig = FileConfig.of(configPath, TomlFormat.instance());
                                fileConfig.load();
                                serverContent = NightConfigParser.configToString(fileConfig);
                            } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                                FileConfig fileConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
                                fileConfig.load();
                                serverContent = NightConfigParser.configToString(fileConfig);
                            } else if (filename.endsWith(".json5")) {
                                serverContent = Json5Parser.json5ToString(configPath);
                            } else if (filename.endsWith(".hocon")) {
                                FileConfig fileConfig = FileConfig.of(configPath, HoconFormat.instance());
                                fileConfig.load();
                                serverContent = NightConfigParser.configToString(fileConfig);
                            } else if (filename.endsWith(".ini")) {
                                serverContent = IniParser.iniToString(configPath);
                            } else if (filename.endsWith(".properties") || filename.endsWith(".conf") || filename.endsWith(".cfg")) {
                                serverContent = PropertiesParser.propertiesToString(configPath);
                            } else if (filename.endsWith(".txt")) {
                                serverContent = TxtConfigParser.txtToString(configPath);
                            } else {
                                continue;
                            }
                            serverConfigs.put(filename, serverContent);
                        } catch (Exception e) {
                            LOGGER.error("Failed to read server config: " + filename, e);
                        }
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
                    context.player().networkHandler.disconnect(
                            Text.literal("Config mismatch! Please make sure these configs match the server: " + files)
                    );
                }
            });
        });
    }
}