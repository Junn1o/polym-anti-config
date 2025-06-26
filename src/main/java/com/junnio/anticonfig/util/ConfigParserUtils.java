
package com.junnio.anticonfig.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class ConfigParserUtils {
    private static final ObjectMapper SHARED_MAPPER = createObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigParserUtils");

    private ConfigParserUtils() {}

    public static ObjectMapper getMapper() {
        return SHARED_MAPPER;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        return mapper;
    }

    public static Object normalizeValue(String value) {
        if (value == null) return null;
        value = value.trim();

        if (value.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (value.equalsIgnoreCase("false")) return Boolean.FALSE;

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    public static void validateRestrictedValue(String filename, Map<String, Object> restrictions) {
        ConfigFormat format = ConfigFormat.fromFilename(filename);
        if (format == null) {
            LOGGER.warn("Unsupported file format for {}", filename);
            return;
        }

        for (Map.Entry<String, Object> entry : restrictions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (format) {
                case INI, PROPERTIES -> validateFlatFormat(key, value);
                case JSON, JSON5, YAML, TOML, HOCON -> validateNestedFormat(key, value);
                case TXT -> validateTxtFormat(key, value);
                default -> LOGGER.warn("No validation rule for format: {}", format);
            }
        }
    }

    private static void validateFlatFormat(String key, Object value) {
        if (key.contains(".")) {
            LOGGER.warn("INI/Properties key contains dots which may cause issues: {}", key);
        }
    }

    private static void validateNestedFormat(String key, Object value) {
        // Validate path format for nested structures
        String[] parts = key.split("\\.");
        for (String part : parts) {
            if (part.isEmpty()) {
                LOGGER.warn("Invalid empty path segment in key: {}", key);
            }
        }
    }

    private static void validateTxtFormat(String key, Object value) {
        if (key.contains(":")) {
            LOGGER.warn("TXT key contains colons which may cause parsing issues: {}", key);
        }
    }
}