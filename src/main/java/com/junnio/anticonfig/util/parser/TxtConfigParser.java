
package com.junnio.anticonfig.util.parser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TxtConfigParser {
    public static String txtToString(Path filePath) throws Exception {
        Map<String, Object> configMap = new HashMap<>();

        // Read all lines from the file
        Files.readAllLines(filePath).forEach(line -> {
            // Skip empty lines
            if (line.trim().isEmpty()) {
                return;
            }

            // Split by first colon
            String[] parts = line.trim().split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Parse boolean values
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    configMap.put(key, Boolean.parseBoolean(value));
                }
                // Parse integer values
                else if (value.matches("\\d+")) {
                    configMap.put(key, Integer.parseInt(value));
                }
                // Keep as string for other values
                else {
                    configMap.put(key, value);
                }
            }
        });

        // Convert to JSON string for consistency with other config formats
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(configMap);
    }
}