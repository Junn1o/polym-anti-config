package com.junnio.anticonfig.util;

import com.junnio.anticonfig.config.ModConfig;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class ConfigValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigValidator");

    public static ValidationResult validateConfigs(Map<String, String> clientConfigs, Iterable<String> configsToCheck) {
        boolean mismatch = false;
        Map<String, String> serverConfigs = new HashMap<>();
        Set<String> mismatchedConfigs = new HashSet<>();

        for (String filename : configsToCheck) {
            Path configPath = ModConfig.resolveConfigPath(filename);
            String serverContent = ConfigFileReader.readConfig(configPath, filename);

            if (serverContent != null) {
                serverConfigs.put(filename, serverContent);
                String clientContent = clientConfigs.get(filename);

                if (!serverContent.equals(clientContent)) {
                    mismatch = true;
                    mismatchedConfigs.add(filename);
                }
            }
        }

        return new ValidationResult(mismatch, serverConfigs, mismatchedConfigs);
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