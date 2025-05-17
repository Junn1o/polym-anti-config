package com.junnio.antifreecam.net;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.junnio.antifreecam.config.ConfigSync;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.data.DataProvider.LOGGER;

public class NetworkManager {
    public static void init() {
        // Server sends configs to client during login
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            Map<String, String> serverConfigs = new HashMap<>();
            Path configDir = FabricLoader.getInstance().getConfigDir();

            // Load server configs
            Path freecamPath = configDir.resolve("freecam.json");
            if (Files.exists(freecamPath)) {
                FileConfig config = FileConfig.of(freecamPath, JsonFormat.minimalInstance());
                config.load();
                serverConfigs.put("freecam.json", config.valueMap().toString());
            }

            Path barsPath = configDir.resolve("leavemybarsalone-client.toml");
            if (Files.exists(barsPath)) {
                FileConfig config = FileConfig.of(barsPath, TomlFormat.instance());
                config.load();
                serverConfigs.put("leavemybarsalone-client.toml", config.valueMap().toString());
            }

            // Send server configs to client
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeMap(serverConfigs, PacketByteBuf::writeString, PacketByteBuf::writeString);
            sender.sendPacket(ConfigSync.CONFIG_SYNC_ID, buf);
        });

        // Server receives and validates client configs
        ServerLoginNetworking.registerGlobalReceiver(ConfigSync.CONFIG_SYNC_ID, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!understood) {
                LOGGER.warn("Client doesn't have the mod installed");
                return;
            }

            // Read client configs
            Map<String, String> clientConfigs = buf.readMap(PacketByteBuf::readString, PacketByteBuf::readString);

            // Load server configs for comparison
            Map<String, String> serverConfigs = new HashMap<>();
            Path configDir = FabricLoader.getInstance().getConfigDir();

            // Check each config file
            boolean mismatch = false;
            StringBuilder mismatched = new StringBuilder();

            for (Map.Entry<String, String> entry : clientConfigs.entrySet()) {
                String filename = entry.getKey();
                String clientContent = entry.getValue();

                Path configPath = configDir.resolve(filename);
                if (Files.exists(configPath)) {
                    FileConfig serverConfig;
                    if (filename.endsWith(".json")) {
                        serverConfig = FileConfig.of(configPath, JsonFormat.minimalInstance());
                    } else if (filename.endsWith(".toml")) {
                        serverConfig = FileConfig.of(configPath, TomlFormat.instance());
                    } else {
                        continue;
                    }
                    serverConfig.load();

                    if (!serverConfig.valueMap().toString().equals(clientContent)) {
                        mismatch = true;
                        mismatched.append(filename).append(", ");
                    }
                }
            }

            if (mismatch) {
                String files = mismatched.substring(0, mismatched.length() - 2);
                handler.disconnect(Text.literal("Config mismatch! Please make sure these configs match the server: " + files));
            }
        });
    }
}