package com.junnio.anticonfig;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.junnio.anticonfig.config.ConfigSync;
import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.net.ConfigScreenSync;
import com.junnio.anticonfig.net.ConfigSyncPayload;
import me.shedaniel.clothconfig2.api.ConfigScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
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
                    FileConfig config;
                    if (filename.endsWith(".json")) {
                        config = FileConfig.of(configPath, JsonFormat.minimalInstance());
                    } else if (filename.endsWith(".toml")) {
                        config = FileConfig.of(configPath, TomlFormat.instance());
                    } else if(filename.endsWith(".yaml") || filename.endsWith(".yml")){
                        config = FileConfig.of(configPath, YamlFormat.defaultInstance());
                    } else {
                        continue;
                    }
                    config.load();
                    configsToSync.put(filename, config.valueMap().toString());
                }
            }

            ConfigSyncPayload payload = new ConfigSyncPayload(configsToSync);
            ClientPlayNetworking.send(payload);

        } catch (Exception e) {
            LOGGER.error("Failed to sync configs after screen close", e);
        }
    }
}