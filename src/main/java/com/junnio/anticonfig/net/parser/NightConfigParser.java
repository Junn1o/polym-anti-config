
package com.junnio.anticonfig.net.parser;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class NightConfigParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String configToString(UnmodifiableConfig config) throws Exception {
        Map<String, Object> configMap = new HashMap<>();

        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Config) {
                // Handle nested configs recursively
                configMap.put(key, configToMap((Config) value));
            } else {
                configMap.put(key, value);
            }
        }

        return MAPPER.writeValueAsString(configMap);
    }

    private static Map<String, Object> configToMap(Config config) {
        Map<String, Object> map = new HashMap<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Config) {
                map.put(key, configToMap((Config) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }
}