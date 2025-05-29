package com.junnio.anticonfig.util;

import com.junnio.anticonfig.config.ModConfig;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigValidator");

    public static ValidationResult validateConfigs(Map<String, String> clientConfigs, Iterable<String> configsToCheck) {
        boolean mismatch = false;
        StringBuilder mismatched = new StringBuilder();
        Map<String, String> serverConfigs = new HashMap<>();

        for (String filename : configsToCheck) {
            Path configPath = ModConfig.resolveConfigPath(filename);
            String serverContent = ConfigFileReader.readConfig(configPath, filename);

            if (serverContent != null) {
                serverConfigs.put(filename, serverContent);
                String clientContent = clientConfigs.get(filename);

                if (!serverContent.equals(clientContent)) {
                    mismatch = true;
                    try {
                        String diff = ConfigDiffUtil.findDifferences(filename, serverContent, clientContent);
                        if (diff != null) {
                            mismatched.append(diff);
                        }
                    } catch (Exception e) {
                        mismatched.append(filename).append(" (failed to compare configs), ");
                    }
                }
            }
        }

        return new ValidationResult(mismatch, mismatched.toString(), serverConfigs);
    }

    public static class ValidationResult {
        private final boolean mismatch;
        private final String mismatchMessage;
        private final Map<String, String> serverConfigs;

        public ValidationResult(boolean mismatch, String mismatchMessage, Map<String, String> serverConfigs) {
            this.mismatch = mismatch;
            this.mismatchMessage = mismatchMessage;
            this.serverConfigs = serverConfigs;
        }

        public boolean hasMismatch() {
            return mismatch;
        }

        public Text getDisconnectMessage() {
            return Text.literal("Config mismatch!\n" + mismatchMessage);
        }

        public Map<String, String> getServerConfigs() {
            return serverConfigs;
        }
    }
}