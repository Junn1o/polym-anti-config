package com.junnio.anticonfig.util.parser;

import com.junnio.anticonfig.util.ConfigParserUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.TreeMap;

public final class PropertiesParser {
    public static String propertiesToString(Path filePath) throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(filePath)) {
            properties.load(reader);
        }

        TreeMap<String, Object> map = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            map.put(key, ConfigParserUtils.normalizeValue(value));
        }

        return ConfigParserUtils.getMapper().writeValueAsString(map);
    }
}