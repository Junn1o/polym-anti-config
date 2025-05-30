package com.junnio.anticonfig.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("AntiConfig");
    private static final String CONFIG_FILE = "anticonfig.json";
    private static ModConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private List<String> configFilesToCheck = new ArrayList<>();
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

    public static void load() {
        if (INSTANCE != null) {
            INSTANCE.validateAndFixPaths();
        }
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
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

}