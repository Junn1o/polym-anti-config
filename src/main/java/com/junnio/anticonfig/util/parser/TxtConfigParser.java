package com.junnio.anticonfig.util.parser;

import com.junnio.anticonfig.util.ConfigParserUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

public final class TxtConfigParser {
    public static String txtToString(Path filePath) throws Exception {
        TreeMap<String, Object> configMap = new TreeMap<>();

        Files.readAllLines(filePath).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(line -> {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String key = line.substring(0, colonIndex).trim();
                        String value = line.substring(colonIndex + 1).trim();
                        configMap.put(key, ConfigParserUtils.normalizeValue(value));
                    }
                });

        return ConfigParserUtils.getMapper().writeValueAsString(configMap);
    }
}