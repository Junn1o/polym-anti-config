package com.junnio.anticonfig.util;

import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.net.ConfigSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigSyncHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigSyncHelper");
    private static Map<String, String> lastServerConfigs = new HashMap<>();

    public static Map<String, String> readConfigsForSync(Iterable<String> configsToRead) {
        Map<String, String> configs = new HashMap<>();

        for (String filename : configsToRead) {
            Path configPath = ModConfig.resolveConfigPath(filename);
            String content = ConfigFileReader.readConfig(configPath, filename);
            if (content != null) {
                configs.put(filename, content);
            }
        }
        return configs;
    }

    public static PacketByteBuf createConfigSyncPacket(Map<String, String> configs) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeMap(configs, PacketByteBuf::writeString, PacketByteBuf::writeString);
        return buf;
    }

    public static void setServerConfigs(Map<String, String> serverConfigs) {
        lastServerConfigs = new HashMap<>(serverConfigs);
    }

    public static void onConfigScreenClose() {
        if (MinecraftClient.getInstance().player != null) {
            Map<String, String> configsToSync = readConfigsForSync(lastServerConfigs.keySet());
            ConfigSyncPayload payload = new ConfigSyncPayload(configsToSync);
            ClientPlayNetworking.send(payload);
        }
    }
}