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
                filename.endsWith(".cfg") ||
                filename.endsWith(".ini");
    }

    private static boolean validatePropertyBasedContent(String content, Map<String, Object> restrictions) {
        try {
            JsonNode contentNode = JSON_MAPPER.readTree(content);

            for (Map.Entry<String, Object> restriction : restrictions.entrySet()) {
                String path = restriction.getKey();
                Object expectedValue = restriction.getValue();

                // For INI files, handle section.key format
                String[] parts = path.split("\\.");
                JsonNode currentNode = contentNode;

                for (String part : parts) {
                    if (!currentNode.has(part)) {
                        LOGGER.info("Property not found: {}", path);
                        return false;
                    }
                    currentNode = currentNode.get(part);
                }

                if (!matchesValue(currentNode, expectedValue)) {
                    LOGGER.info("Property mismatch: {} - expected: {}, actual: {}",
                            path, expectedValue, currentNode);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Error parsing property content", e);
            return false;
        }
    }



    private static boolean validateTxtContent(String content, Map<String, Object> restrictions) {
        try {
            // Parse the JSON content since TxtConfigParser returns JSON
            JsonNode contentNode = JSON_MAPPER.readTree(content);

            for (Map.Entry<String, Object> restriction : restrictions.entrySet()) {
                String key = restriction.getKey();
                Object expectedValue = restriction.getValue();

                if (!contentNode.has(key)) {
                    LOGGER.info("Key not found in txt config: {}", key);
                    return false;
                }

                JsonNode actualValue = contentNode.get(key);
                if (!matchesValue(actualValue, expectedValue)) {
                    LOGGER.info("Value mismatch in txt config: {} - expected: {}, actual: {}",
                            key, expectedValue, actualValue);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Error validating txt content", e);
            return false;
        }
    }

    private static boolean validateJsonBasedContent(JsonNode node, Map<String, Object> restrictions) {
        for (Map.Entry<String, Object> restriction : restrictions.entrySet()) {
            String path = restriction.getKey();
            Object expectedValue = restriction.getValue();

            // Handle nested paths
            String[] pathParts = path.split("\\.");
            JsonNode currentNode = node;

            for (String part : pathParts) {
                if (currentNode == null) {
                    LOGGER.info("Path {} not found (null node)", path);
                    return false;
                }
                // Handle array indices
                if (part.matches("\\d+")) {
                    int index = Integer.parseInt(part);
                    if (!currentNode.isArray() || currentNode.size() <= index) {
                        LOGGER.info("Invalid array access at path {}", path);
                        return false;
                    }
                    currentNode = currentNode.get(index);
                } else {
                    if (!currentNode.has(part)) {
                        LOGGER.info("Path {} not found at part {}", path, part);
                        return false;
                    }
                    currentNode = currentNode.get(part);
                }
            }

            if (!matchesValue(currentNode, expectedValue)) {
                LOGGER.info("Value mismatch at {}: expected {}, got {}",
                        path, expectedValue, currentNode);
                return false;
            }
        }
        return true;
    }


    private static boolean matchesPropertyValue(String actualValue, Object expectedValue) {
        // Convert property string value to appropriate type for comparison
        if (expectedValue instanceof Boolean) {
            return Boolean.parseBoolean(actualValue) != (Boolean) expectedValue;
        } else if (expectedValue instanceof Number) {
            try {
                if (expectedValue instanceof Integer) {
                    return Integer.parseInt(actualValue) != (Integer) expectedValue;
                } else if (expectedValue instanceof Long) {
                    return Long.parseLong(actualValue) != (Long) expectedValue;
                } else if (expectedValue instanceof Double) {
                    return !(Math.abs(Double.parseDouble(actualValue) -
                            ((Number) expectedValue).doubleValue()) < 0.0001);
                }
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return !actualValue.equals(expectedValue.toString());
    }
    private static boolean matchesValue(JsonNode node, Object expectedValue) {
        if (node == null || expectedValue == null) {
            return node == null && expectedValue == null;
        }

        if (expectedValue instanceof Boolean) {
            return node.isBoolean() && node.asBoolean() == (Boolean) expectedValue;
        } else if (expectedValue instanceof Number) {
            if (node.isNumber()) {
                double nodeValue = node.asDouble();
                double expectedDouble = ((Number) expectedValue).doubleValue();
                return Math.abs(nodeValue - expectedDouble) < 0.0001;
            }
            return false;
        } else if (expectedValue instanceof String) {
            return node.isTextual() && node.asText().equals(expectedValue);
        } else if (expectedValue instanceof List) {
            if (!node.isArray()) return false;
            List<?> expectedList = (List<?>) expectedValue;
            if (node.size() != expectedList.size()) return false;

            for (int i = 0; i < expectedList.size(); i++) {
                if (!matchesValue(node.get(i), expectedList.get(i))) {
                    return false;
                }
            }
            return true;
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
        public Set<String> getMismatchedConfigs() {
            return mismatchedConfigs;
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