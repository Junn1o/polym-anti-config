package com.junnio.anticonfig.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.junnio.anticonfig.util.ConfigFormat;
import com.junnio.anticonfig.util.ConfigParserUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("AntiConfig");
    private static final String CONFIG_FILE = "anticonfig.json";
    private static ModConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private List<String> configFilesToCheck = new ArrayList<>();
    private Map<String, Map<String, Object>> restrictedValues = new HashMap<>();
    public Map<String, Map<String, Object>> getRestrictedValues() {
        return restrictedValues;
    }

    public ModConfig() {

    }

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public List<String> getConfigFilesToCheck() {
        return configFilesToCheck;
    }

    private void setupDefaultRestrictions() {
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
                INSTANCE.validateConfig();
                LOGGER.info("Loaded AntiConfig config");
            } catch (IOException e) {
                LOGGER.error("Failed to read config file", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }


    public static void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try {
            String json = GSON.toJson(INSTANCE);
            Files.writeString(configPath, json);
            LOGGER.info("Saved AntiConfig config");
        } catch (IOException e) {
            LOGGER.error("Failed to save config file", e);
        }
    }
    public static Path resolveConfigPath(String relativePath) {
        return FabricLoader.getInstance().getConfigDir().resolve(relativePath);
    }

    public void validateAndFixPaths() {
        List<String> validPaths = new ArrayList<>();
        for (String path : configFilesToCheck) {
            String normalizedPath = path.replace('\\', '/');
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
            if (!normalizedPath.contains("..")) {
                validPaths.add(normalizedPath);
            } else {
                LOGGER.warn("Ignoring invalid config path that tries to escape config directory: {}", path);
            }
        }
        configFilesToCheck = validPaths;
    }
    public void validateConfig() {
        validateAndFixPaths();

        // Validate restricted values format
        for (Map.Entry<String, Map<String, Object>> entry : restrictedValues.entrySet()) {
            String filename = entry.getKey();
            Map<String, Object> restrictions = entry.getValue();

            // Validate the format is supported
            if (ConfigFormat.fromFilename(filename) == null) {
                LOGGER.warn("Unsupported file format in restrictedValues: {}", filename);
                continue;
            }

            // Validate the restrictions format
            ConfigParserUtils.validateRestrictedValue(filename, restrictions);
        }
    }

}