package com.junnio.anticonfig.util.parser;

import com.junnio.anticonfig.util.ConfigParserUtils;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.TreeMap;

public final class IniParser {
    public static String iniToString(Path filePath) throws Exception {
        INIConfiguration ini = new INIConfiguration();
        try (FileReader reader = new FileReader(filePath.toFile())) {
            ini.read(reader);
        }

        TreeMap<String, Object> resultMap = new TreeMap<>();

        // Process default section
        TreeMap<String, Object> defaultSection = processSection(ini.getSection(null));
        if (!defaultSection.isEmpty()) {
            resultMap.put("default", defaultSection);
        }

        // Process named sections
        for (String sectionName : ini.getSections()) {
            resultMap.put(sectionName, processSection(ini.getSection(sectionName)));
        }

        return ConfigParserUtils.getMapper().writeValueAsString(resultMap);
    }

    private static TreeMap<String, Object> processSection(SubnodeConfiguration section) {
        TreeMap<String, Object> sectionMap = new TreeMap<>();
        section.getKeys().forEachRemaining(key ->
                sectionMap.put(key, ConfigParserUtils.normalizeValue(section.getString(key)))
        );
        return sectionMap;
    }
}