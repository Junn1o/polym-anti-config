
package com.junnio.anticonfig.util;

public enum ConfigFormat {
    JSON(".json"),
    JSON5(".json5"),
    TOML(".toml"),
    YAML(".yml", ".yaml"),
    HOCON(".hocon", ".conf"),
    INI(".ini"),
    PROPERTIES(".properties"),
    CFG(".cfg"),
    TXT(".txt");

    private final String[] extensions;

    ConfigFormat(String... extensions) {
        this.extensions = extensions;
    }

    public static ConfigFormat fromFilename(String filename) {
        for (ConfigFormat format : values()) {
            for (String ext : format.extensions) {
                if (filename.endsWith(ext)) {
                    return format;
                }
            }
        }
        return null;
    }
}