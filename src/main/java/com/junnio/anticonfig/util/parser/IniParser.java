package com.junnio.anticonfig.util.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IniParser {
    public static String iniToString(Path filePath) throws Exception {
        INIConfiguration ini = new INIConfiguration();
        ini.read(new FileReader(filePath.toFile()));

        // Create a nested map structure to represent sections and their properties
        Map<String, Object> resultMap = new HashMap<>();

        // Get all section names
        Iterator<String> sectionNames = ini.getSections().iterator();

        // Add the default section (properties not in any section)
        Map<String, Object> defaultSection = new HashMap<>();
        ini.getSection(null).getKeys().forEachRemaining(key -> defaultSection.put(key, ini.getProperty(key)));
        if (!defaultSection.isEmpty()) {
            resultMap.put("default", defaultSection);
        }

        // Process each section
        while (sectionNames.hasNext()) {
            String sectionName = sectionNames.next();
            SubnodeConfiguration section = ini.getSection(sectionName);

            Map<String, Object> sectionMap = new HashMap<>();
            section.getKeys().forEachRemaining(key -> sectionMap.put(key, section.getProperty(key)));

            resultMap.put(sectionName, sectionMap);
        }

        // Convert to JSON string for consistency with other config formats
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(resultMap);
    }
}
