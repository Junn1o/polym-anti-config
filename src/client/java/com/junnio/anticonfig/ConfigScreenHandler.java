package com.junnio.anticonfig;

import com.junnio.anticonfig.config.ConfigSync;
import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.net.ConfigScreenSync;
import com.junnio.anticonfig.net.ConfigSyncPayload;
import me.shedaniel.clothconfig2.api.ConfigScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;

import java.util.HashMap;
import java.util.Map;

public class ConfigScreenHandler {
    public static void init() {
        // Register the payload type on client side
        PayloadTypeRegistry.playC2S().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);
    }

    public static void onConfigScreenClose() {
        // Create config map
        Map<String, String> configsToSync = new HashMap<>();
        ModConfig config = ModConfig.getInstance();

        // Gather current config states
        for (String filename : config.getConfigFilesToCheck()) {
            String content = ConfigSync.getConfigContent(filename);
            if (content != null) {
                configsToSync.put(filename, content);
            }
        }

        // Create and send payload
        ConfigSyncPayload payload = new ConfigSyncPayload(configsToSync);
        ClientPlayNetworking.send(payload);
    }
}