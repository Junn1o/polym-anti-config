package com.junnio.anticonfig.util.parser;

import com.junnio.anticonfig.util.ConfigParserUtils;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public final class IniParser {
    public static String iniToString(Path filePath) throws Exception {
        INIConfiguration ini = new INIConfiguration();
        try (FileReader reader = new FileReader(filePath.toFile())) {
            ini.read(reader);
        }

        TreeMap<String, Object> resultMap = new TreeMap<>();

        // Process default section
        processSection(ini.getSection(null), "", resultMap);

        // Process named sections
        for (String sectionName : ini.getSections()) {
            processSection(ini.getSection(sectionName), sectionName, resultMap);
        }

        return ConfigParserUtils.getMapper().writeValueAsString(resultMap);
    }

    private static void processSection(SubnodeConfiguration section, String sectionPrefix, Map<String, Object> resultMap) {
        section.getKeys().forEachRemaining(key -> {
            String fullKey = sectionPrefix.isEmpty() ? key : sectionPrefix + "." + key;
            resultMap.put(fullKey, ConfigParserUtils.normalizeValue(section.getString(key)));
        });
    }
}