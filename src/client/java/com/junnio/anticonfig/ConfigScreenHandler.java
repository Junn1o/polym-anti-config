package com.junnio.anticonfig;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.hocon.HoconFormat;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.junnio.anticonfig.net.ConfigSyncPayload;
import com.junnio.anticonfig.net.parser.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigScreenHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigScreenHandler");
    private static Map<String, String> lastServerConfigs = new HashMap<>();

    public static void init() {
        PayloadTypeRegistry.playC2S().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
    }

    // Method to store server's config list when received during login
    public static void setServerConfigs(Map<String, String> serverConfigs) {
        lastServerConfigs = new HashMap<>(serverConfigs);
    }

    public static void onConfigScreenClose() {
        Map<String, String> configsToSync = new HashMap<>();
        Path configDir = FabricLoader.getInstance().getConfigDir();

        try {
            // Only read configs that were requested by server during login
            for (String filename : lastServerConfigs.keySet()) {
                Path configPath = configDir.resolve(filename);
                if (Files.exists(configPath)) {
                    try {
                        String serverContent;
                        if (filename.endsWith(".json")) {
                            FileConfig fileConfig = FileConfig.of(configPath, JsonFormat.fancyInstance());
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
                        configsToSync.put(filename, serverContent);
                    } catch (Exception e) {
                        LOGGER.error("Failed to read server config: " + filename, e);
                    }
                }
            }

            ConfigSyncPayload payload = new ConfigSyncPayload(configsToSync);
            ClientPlayNetworking.send(payload);

        } catch (Exception e) {
            LOGGER.error("Failed to sync configs after screen close", e);
        }
    }
}