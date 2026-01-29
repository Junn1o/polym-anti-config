package com.junnio.anticonfig.util.parser;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.junnio.anticonfig.util.ConfigParserUtils;

import java.util.TreeMap;

public final class NightConfigParser {
    public static String configToString(UnmodifiableConfig config) throws Exception {
        return ConfigParserUtils.getMapper().writeValueAsString(configToMap(config));
    }

    private static TreeMap<String, Object> configToMap(UnmodifiableConfig config) {
        TreeMap<String, Object> map = new TreeMap<>();

        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Config) {
                map.put(key, configToMap((Config) value));
            } else if (value instanceof String) {
                map.put(key, ConfigParserUtils.normalizeValue((String) value));
            } else {
                map.put(key, value);
            }
        }

        return map;
    }
}