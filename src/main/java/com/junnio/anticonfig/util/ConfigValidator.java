package com.junnio.anticonfig.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junnio.anticonfig.config.ModConfig;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                if (clientContent != null) {
                    if (ConfigParserUtils.PARSE_ERROR_MARKER.equals(clientContent)) {
                        LOGGER.error("Client config file {} exists but is malformed", filename);
                        mismatch = true;
                        mismatchedConfigs.add(filename);
                        continue;
                    }

                    if (!validateConfig(filename, clientContent, serverContent, restrictedValues)) {
                        mismatch = true;
                        mismatchedConfigs.add(filename);
                    }
                }
            }
        }

        return new ValidationResult(mismatch, serverConfigs, mismatchedConfigs);
    }

    private static boolean validateConfig(String filename, String clientContent, String serverContent,
                                          Map<String, Map<String, Object>> restrictedValues) {
        try {
            if (!restrictedValues.containsKey(filename)) {
                return serverContent.equals(clientContent);
            }

            LOGGER.info("Validating {} with content: {}", filename, clientContent);
            Map<String, Object> restrictions = restrictedValues.get(filename);

            ConfigFormat format = ConfigFormat.fromFilename(filename);
            if (format == null) {
                LOGGER.error("Unsupported file format for: {}", filename);
                return false;
            }

            JsonNode clientNode = JSON_MAPPER.readTree(clientContent);
            return validateByFormat(format, clientNode, restrictions);
        } catch (Exception e) {
            LOGGER.error("Error validating config: " + filename, e);
            return false;
        }
    }
    private static boolean validateByFormat(ConfigFormat format, JsonNode content, Map<String, Object> restrictions) {
        return switch (format) {
            case INI, PROPERTIES, CFG -> validateFlatContent(content, restrictions);
            case JSON, JSON5, YAML, TOML, HOCON -> validateNestedContent(content, restrictions);
            case TXT -> validateTxtContent(content, restrictions);
        };
    }
    private static boolean validateFlatContent(JsonNode content, Map<String, Object> restrictions) {
        for (Map.Entry<String, Object> restriction : restrictions.entrySet()) {
            String key = restriction.getKey();
            Object expectedValue = restriction.getValue();

            // Handle flat key-value structure
            if (!content.has(key)) {
                LOGGER.info("Key not found: {}", key);
                return false;
            }

            if (matchesValue(content.get(key), expectedValue)) {
                LOGGER.info("Value mismatch for {}: expected {}, got {}",
                        key, expectedValue, content.get(key));
                return false;
            }
        }
        return true;
    }

    private static boolean validateNestedContent(JsonNode content, Map<String, Object> restrictions) {
        for (Map.Entry<String, Object> restriction : restrictions.entrySet()) {
            String path = restriction.getKey();
            Object expectedValue = restriction.getValue();

            JsonNode currentNode = content;
            for (String part : path.split("\\.")) {
                if (currentNode == null || !currentNode.has(part)) {
                    LOGGER.info("Path not found: {}", path);
                    return false;
                }
                currentNode = currentNode.get(part);
            }

            if (matchesValue(currentNode, expectedValue)) {
                LOGGER.info("Value mismatch at {}: expected {}, got {}",
                        path, expectedValue, currentNode);
                return false;
            }
        }
        return true;
    }

    private static boolean validateTxtContent(JsonNode content, Map<String, Object> restrictions) {
        return validateFlatContent(content, restrictions); // TXT files are handled as flat key-value pairs
    }

    private static boolean matchesValue(JsonNode node, Object expectedValue) {
        if (node == null || expectedValue == null) {
            return node != null || expectedValue != null;
        }

        // First, validate type compatibility
        if (expectedValue instanceof Boolean) {
            if (!node.isBoolean()) {
                LOGGER.info("Type mismatch: expected boolean, got {}", node.getNodeType());
                return true;
            }
            return node.asBoolean() != (Boolean) expectedValue;
        } else if (expectedValue instanceof Number) {
            if (!node.isNumber()) {
                LOGGER.info("Type mismatch: expected number, got {}", node.getNodeType());
                return true;
            }
            double nodeValue = node.asDouble();
            double expectedDouble = ((Number) expectedValue).doubleValue();
            return !(Math.abs(nodeValue - expectedDouble) < 0.0001);
        } else if (expectedValue instanceof String) {
            if (!node.isTextual()) {
                LOGGER.info("Type mismatch: expected string, got {}", node.getNodeType());
                return true;
            }
            return !node.asText().equals(expectedValue);
        } else if (expectedValue instanceof List<?> expectedList) {
            if (!node.isArray()) {
                LOGGER.info("Type mismatch: expected array, got {}", node.getNodeType());
                return true;
            }
            if (node.size() != expectedList.size()) {
                LOGGER.info("Array size mismatch: expected {}, got {}", expectedList.size(), node.size());
                return true;
            }
            for (int i = 0; i < expectedList.size(); i++) {
                if (matchesValue(node.get(i), expectedList.get(i))) {
                    return true;
                }
            }
            return false;
        }

        LOGGER.warn("Unsupported value type: {}", expectedValue.getClass());
        return true;
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