package com.junnio.anticonfig.util.parser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesParser {
    public static String propertiesToString(Path filePath) throws Exception {
        // Read properties file
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(filePath));

        // Convert Properties to Map
        Map<String, Object> map = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }

        // Convert to JSON string for consistency with other config formats
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(map);
    }
}

