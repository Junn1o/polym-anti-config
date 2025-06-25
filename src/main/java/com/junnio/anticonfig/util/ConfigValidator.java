package com.junnio.anticonfig.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junnio.anticonfig.config.ModConfig;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;

public class ConfigValidator {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigValidator");
    public static ValidationResult validateConfigs(Map<String, String> clientConfigs, Iterable<String> configsToCheck) {
        boolean mismatch = false;
        Map<String, String> serverConfigs = new HashMap<>();
        Set<String> mismatchedConfigs = new HashSet<>();

        ModConfig modConfig = ModConfig.getInstance();
        Map<String, Map<String, Object>> restrictedValues = modConfig.getRestrictedValues();

        for (String filename : configsToCheck) {
            Path configPath = ModConfig.resolveConfigPath(filename);
            String serverContent = ConfigFileReader.readConfig(configPath, filename);

            if (serverContent != null) {
                serverConfigs.put(filename, serverContent);
                String clientContent = clientConfigs.get(filename);

                if (clientContent != null && !validateRestrictedValues(filename, clientContent, serverContent, restrictedValues)) {
                    mismatch = true;
                    mismatchedConfigs.add(filename);
                }
            }
        }

        return new ValidationResult(mismatch, serverConfigs, mismatchedConfigs);
    }



    private static boolean validateRestrictedValues(String filename, String clientContent, String serverContent,
                                                    Map<String, Map<String, Object>> restrictedValues) {
        try {
            if (!restrictedValues.containsKey(filename)) {
                return serverContent.equals(clientContent);
            }

            LOGGER.info("Validating {} with content: {}", filename, clientContent);
            Map<String, Object> fileRestrictions = restrictedValues.get(filename);

            // Handle different file formats
            if (isPropertyBasedFormat(filename)) {
                return validatePropertyBasedContent(clientContent, fileRestrictions);
            } else if (filename.endsWith(".txt")) {
                return validateTxtContent(clientContent, fileRestrictions);
            } else {
                // Handle JSON-like formats (JSON, JSON5, YAML, TOML, HOCON)
                JsonNode clientJson = JSON_MAPPER.readTree(clientContent);
                return validateJsonBasedContent(clientJson, fileRestrictions);
            }
        } catch (Exception e) {
            LOGGER.error("Error validating restricted values for " + filename, e);
            return false;
        }
    }

    private static boolean isPropertyBasedFormat(String filename) {
        return filename.endsWith(".properties") ||
                filename.endsWith(".conf") ||
                filename.endsWith(".cfg") ||
                filename.endsWith(".ini");
    }

    private static boolean validatePropertyBasedContent(String content, Map<String, Object> restrictions) {
        // Convert content to properties for easier comparison
        Properties props = new Properties();
        try (StringReader reader = new StringReader(content)) {
            props.load(reader);
        } catch (IOException e) {
            LOGGER.error("Error parsing property content", e);
            return false;
        }

        for (Map.Entry<String, Object> restriction : restrictions.entrySet()) {
            String key = restriction.getKey();
            Object expectedValue = restriction.getValue();
            String actualValue = props.getProperty(key);

            if (actualValue == null || !matchesPropertyValue(actualValue, expectedValue)) {
                LOGGER.info("Property mismatch: {} - expected: {}, actual: {}",
                        key, expectedValue, actualValue);
                return false;
            }
        }
        return true;
    }

    private static boolean validateTxtContent(String content, Map<String, Object> restrictions) {
        Map<String, String> txtMap = new TreeMap<>();

        // Parse TXT content into map (assuming key:value format)
        content.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(line -> {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String key = line.substring(0, colonIndex).trim();
                        String value = line.substring(colonIndex + 1).trim();
                        txtMap.put(key, value);
                    }
                });

        // Validate restrictions
        for (Map.Entry<String, Object> restriction : restrictions.entrySet()) {
            String key = restriction.getKey();
            Object expectedValue = restriction.getValue();
            String actualValue = txtMap.get(key);

            if (actualValue == null || !matchesPropertyValue(actualValue, expectedValue)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateJsonBasedContent(JsonNode node, Map<String, Object> restrictions) {
        for (Map.Entry<String, Object> restriction : restrictions.entrySet()) {
            String path = restriction.getKey();
            Object expectedValue = restriction.getValue();

            // Handle nested paths
            String[] pathParts = path.split("\\.");
            JsonNode currentNode = node;

            for (String part : pathParts) {
                if (currentNode == null || !currentNode.has(part)) {
                    LOGGER.info("Path {} not found", path);
                    return false;
                }
                currentNode = currentNode.get(part);
            }

            if (!validateJsonNode(currentNode, expectedValue)) {
                LOGGER.info("Value mismatch at {}: expected {}, got {}",
                        path, expectedValue, currentNode);
                return false;
            }
        }
        return true;
    }

    private static boolean validateJsonNode(JsonNode node, Object expectedValue) {
        // Handle special cases first
        if (node.isObject() && node.has("enable")) {
            // For objects with 'enable' field, check the enable value
            return matchesValue(node.get("enable"), expectedValue);
        }

        if (expectedValue instanceof Map && node.isObject()) {
            // Handle nested object validation
            @SuppressWarnings("unchecked")
            Map<String, Object> expectedMap = (Map<String, Object>) expectedValue;
            return validateComplexValue(node, expectedMap);
        }

        if (expectedValue instanceof List && node.isArray()) {
            // Handle array validation
            return validateArrayValue(node, (List<?>) expectedValue);
        }

        // Regular value validation
        return matchesValue(node, expectedValue);
    }
    private static boolean validateComplexValue(JsonNode node, Map<String, Object> expectedMap) {
        for (Map.Entry<String, Object> entry : expectedMap.entrySet()) {
            if (!node.has(entry.getKey()) ||
                    !matchesValue(node.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateArrayValue(JsonNode arrayNode, List<?> expectedList) {
        if (arrayNode.size() != expectedList.size()) {
            return false;
        }

        for (int i = 0; i < arrayNode.size(); i++) {
            if (!matchesValue(arrayNode.get(i), expectedList.get(i))) {
                return false;
            }
        }
        return true;
    }


    private static boolean matchesPropertyValue(String actualValue, Object expectedValue) {
        // Convert property string value to appropriate type for comparison
        if (expectedValue instanceof Boolean) {
            return Boolean.parseBoolean(actualValue) == (Boolean) expectedValue;
        } else if (expectedValue instanceof Number) {
            try {
                if (expectedValue instanceof Integer) {
                    return Integer.parseInt(actualValue) == (Integer) expectedValue;
                } else if (expectedValue instanceof Long) {
                    return Long.parseLong(actualValue) == (Long) expectedValue;
                } else if (expectedValue instanceof Double) {
                    return Math.abs(Double.parseDouble(actualValue) -
                            ((Number) expectedValue).doubleValue()) < 0.0001;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return actualValue.equals(expectedValue.toString());
    }

    private static List<JsonNode> findAllNodesWithKey(JsonNode node, String targetKey) {
        List<JsonNode> results = new ArrayList<>();

        if (node.isObject()) {
            if (node.has(targetKey)) {
                results.add(node.get(targetKey));
            }
            node.fields().forEachRemaining(entry ->
                    results.addAll(findAllNodesWithKey(entry.getValue(), targetKey)));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(element ->
                    results.addAll(findAllNodesWithKey(element, targetKey)));
        }

        return results;
    }

    private static boolean matchesValue(JsonNode node, Object expectedValue) {
        if (expectedValue instanceof Boolean) {
            return node.isBoolean() && node.asBoolean() == (Boolean) expectedValue;
        } else if (expectedValue instanceof Number) {
            if (node.isNumber()) {
                // Handle different number types
                if (expectedValue instanceof Integer) {
                    return node.asInt() == (Integer) expectedValue;
                } else if (expectedValue instanceof Long) {
                    return node.asLong() == (Long) expectedValue;
                } else if (expectedValue instanceof Double) {
                    return Math.abs(node.asDouble() - (Double) expectedValue) < 0.0001;
                } else if (expectedValue instanceof Float) {
                    return Math.abs(node.asDouble() - (Float) expectedValue) < 0.0001;
                }
            }
            return false;
        } else if (expectedValue instanceof String) {
            return node.isTextual() && node.asText().equals(expectedValue);
        }
        return false;
    }

    public static class ValidationResult {
        private final boolean mismatch;
        private final Map<String, String> serverConfigs;
        private final Set<String> mismatchedConfigs;

        public ValidationResult(boolean mismatch, Map<String, String> serverConfigs, Set<String> mismatchedConfigs) {
            this.mismatch = mismatch;
            this.serverConfigs = serverConfigs;
            this.mismatchedConfigs = mismatchedConfigs;
        }


        public boolean hasMismatch() {
            return mismatch;
        }

        public Text getDisconnectMessage() {
            Text startText = Text.translatable("anticonfig.text.startresult");
            StringBuilder configListBuilder = new StringBuilder("\n");
            List<String> sortedMismatches = new ArrayList<>(mismatchedConfigs);
            Collections.sort(sortedMismatches);
            for (int i = 0; i < sortedMismatches.size(); i++) {
                configListBuilder.append("§4").append(sortedMismatches.get(i));
                if (i % 3 != 2 && i != sortedMismatches.size() - 1) {
                    configListBuilder.append(", ");
                }
                if ((i % 3 == 2 || i == sortedMismatches.size() - 1) && i != sortedMismatches.size() - 1) {
                    configListBuilder.append("\n");
                }
            }

            Text configListText = Text.literal(configListBuilder.toString());
            Text endText = Text.translatable("anticonfig.text.endtresult");
            return startText.copy()
                    .append(configListText)
                    .append("\n")
                    .append(endText);
        }
        public Text notifyBypassMessage() {
            return Text.translatable("anticonfig.text.bypassnotify");
        }


        public Map<String, String> getServerConfigs() {
            return serverConfigs;
        }
    }
}