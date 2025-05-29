package com.junnio.anticonfig.net;

import com.junnio.anticonfig.Anticonfig;
import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.util.ConfigValidator;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NetworkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AntiConfig");
    public static final Identifier CONFIG_SYNC_ID = Identifier.of(Anticonfig.MODID, "config_sync");

    public static void init() {
        PayloadTypeRegistry.playC2S().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);

        // Server sends configs to client during login
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            ModConfig config = ModConfig.getInstance();
            ConfigValidator.ValidationResult result = ConfigValidator.validateConfigs(
                    new HashMap<>(),
                    config.getConfigFilesToCheck()
            );

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeMap(result.getServerConfigs(), PacketByteBuf::writeString, PacketByteBuf::writeString);
            sender.sendPacket(CONFIG_SYNC_ID, buf);
        });

        // Server receives and validates client configs
        ServerLoginNetworking.registerGlobalReceiver(CONFIG_SYNC_ID, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!understood) {
                LOGGER.warn("Client doesn't have the mod installed");
                return;
            }

            Map<String, String> clientConfigs = buf.readMap(PacketByteBuf::readString, PacketByteBuf::readString);
            ModConfig config = ModConfig.getInstance();

            ConfigValidator.ValidationResult result = ConfigValidator.validateConfigs(
                    clientConfigs,
                    config.getConfigFilesToCheck()
            );

            if (result.hasMismatch()) {
                handler.disconnect(result.getDisconnectMessage());
            }
        });

        // Handle config updates during gameplay
        ServerPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ModConfig config = ModConfig.getInstance();
                ConfigValidator.ValidationResult result = ConfigValidator.validateConfigs(
                        payload.configs(),
                        config.getConfigFilesToCheck()
                );

                if (result.hasMismatch()) {
                    context.player().networkHandler.disconnect(result.getDisconnectMessage());
                }
            });
        });
    }
}