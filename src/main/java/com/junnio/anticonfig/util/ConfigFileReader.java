package com.junnio.anticonfig.util;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.hocon.HoconFormat;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.junnio.anticonfig.util.parser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigFileReader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigFileReader");

    public static String readConfig(Path configPath, String filename) {
        if (!Files.exists(configPath)) {
            return null;
        }
        try {
            if (filename.endsWith(".json")) {
                FileConfig fileConfig = FileConfig.of(configPath, JsonFormat.fancyInstance());
                fileConfig.load();
                return NightConfigParser.configToString(fileConfig);
            } else if (filename.endsWith(".toml")) {
                FileConfig fileConfig = FileConfig.of(configPath, TomlFormat.instance());
                fileConfig.load();
                return NightConfigParser.configToString(fileConfig);
            } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                FileConfig fileConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
                fileConfig.load();
                return NightConfigParser.configToString(fileConfig);
            } else if (filename.endsWith(".json5")) {
                return Json5Parser.json5ToString(configPath);
            } else if (filename.endsWith(".hocon") || filename.endsWith(".conf")) {
                FileConfig fileConfig = FileConfig.of(configPath, HoconFormat.instance());
                fileConfig.load();
                return NightConfigParser.configToString(fileConfig);
            } else if (filename.endsWith(".ini")) {
                return IniParser.iniToString(configPath);
            } else if (filename.endsWith(".properties") || filename.endsWith(".cfg")) {
                return PropertiesParser.propertiesToString(configPath);
            } else if (filename.endsWith(".txt")) {
                return TxtConfigParser.txtToString(configPath);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read config file: " + filename, e);
        }
        return null;
    }
}