package com.junnio.anticonfig.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class ConfigParserUtils {
    private static final ObjectMapper SHARED_MAPPER = createObjectMapper();

    private ConfigParserUtils() {} // Prevent instantiation

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

        // Boolean handling
        if (value.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (value.equalsIgnoreCase("false")) return Boolean.FALSE;

        // Number handling
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            // Not a number, return as string
            return value;
        }
    }
}