package com.junnio.anticonfig.util;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.hocon.HoconFormat;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.junnio.anticonfig.util.parser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigFileReader {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigFileReader");
    private static final Map<Path, CachedConfig> CACHE = new ConcurrentHashMap<>();

    private record CachedConfig(FileTime lastModified, String content) {
    }

    public static String readConfig(Path configPath, String filename) {
        if (!Files.exists(configPath)) {
            CACHE.remove(configPath);
            return null;
        }

        FileTime lastModified;
        try {
            lastModified = Files.getLastModifiedTime(configPath);
        } catch (IOException e) {
            lastModified = null;
        }

        CachedConfig cached = CACHE.get(configPath);
        if (cached != null && lastModified != null && cached.lastModified().equals(lastModified)) {
            return cached.content();
        }

        String content = parseConfig(configPath, filename);
        if (lastModified != null) {
            CACHE.put(configPath, new CachedConfig(lastModified, content));
        }
        return content;
    }

    private static String parseConfig(Path configPath, String filename) {
        try {
            String result = null;
            if (filename.endsWith(".json")) {
                FileConfig fileConfig = FileConfig.of(configPath, JsonFormat.fancyInstance());
                fileConfig.load();
                result = NightConfigParser.configToString(fileConfig);
// all comment is for maintenance
//            } else if (filename.endsWith(".toml")) {
//                FileConfig fileConfig = FileConfig.of(configPath, TomlFormat.instance());
//                fileConfig.load();
//                result =  NightConfigParser.configToString(fileConfig);
//            } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
//                FileConfig fileConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
//                fileConfig.load();
//                result =  NightConfigParser.configToString(fileConfig);
            } else if (filename.endsWith(".json5")) {
                result = Json5Parser.json5ToString(configPath);
//            } else if (filename.endsWith(".hocon") || filename.endsWith(".conf")) {
//                FileConfig fileConfig = FileConfig.of(configPath, HoconFormat.instance());
//                fileConfig.load();
//                result =  NightConfigParser.configToString(fileConfig);
//            } else if (filename.endsWith(".ini")) {
//                result =  IniParser.iniToString(configPath);
            } else if (filename.endsWith(".properties") || filename.endsWith(".cfg")) {
                result =  PropertiesParser.propertiesToString(configPath);
            } else if (filename.endsWith(".txt")) {
                result =  TxtConfigParser.txtToString(configPath);
            }
            return result != null ? result : ConfigParserUtils.PARSE_ERROR_MARKER;
        } catch (Exception e) {
            LOGGER.error("Failed to read config file: " + filename, e);
            return ConfigParserUtils.PARSE_ERROR_MARKER;
        }
    }
}