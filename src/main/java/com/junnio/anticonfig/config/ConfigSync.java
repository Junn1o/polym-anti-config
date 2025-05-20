package com.junnio.anticonfig.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("AntiConfig");
    public static final Identifier CONFIG_SYNC_ID = Identifier.of("anticonfig", "config_sync");
    private static final Map<String, FileConfig> CONFIGS_TO_CHECK = new HashMap<>();

    public static void registerConfigToCheck(String filename) {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(filename);
        if (Files.exists(configPath)) {
            FileConfig config;
            if (filename.endsWith(".json")) {
                config = FileConfig.of(configPath, JsonFormat.minimalInstance());
            } else if (filename.endsWith(".toml")) {
                config = FileConfig.of(configPath, TomlFormat.instance());
            } else {
                return; // Unsupported format
            }
            config.load();
            CONFIGS_TO_CHECK.put(filename, config);
        }
    }

    public static String getConfigContent(String filename) {
        FileConfig config = CONFIGS_TO_CHECK.get(filename);
        if (config != null) {
            return config.valueMap().toString();
        }
        return null;
    }
    public static void registerConfigsFromModConfig() {
        ModConfig config = ModConfig.getInstance();
        for (String filename : config.getConfigFilesToCheck()) {
            registerConfigToCheck(filename);
        }
    }

}