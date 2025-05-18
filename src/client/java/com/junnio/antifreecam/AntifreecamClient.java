package com.junnio.antifreecam;

import com.junnio.antifreecam.config.ConfigSync;
import com.junnio.antifreecam.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AntifreecamClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("AntiFreecam");
	private static ModConfig config;
	@Override
	public void onInitializeClient() {
		for (String configFile : config.getConfigFilesToCheck()) {
			ConfigSync.registerConfigToCheck(configFile);
		}

		// Register config sync handler
		ClientLoginNetworking.registerGlobalReceiver(ConfigSync.CONFIG_SYNC_ID, (client, handler, buf, listenerAdder) -> {
			// Read server configs
			Map<String, String> serverConfigs = buf.readMap(PacketByteBuf::readString, PacketByteBuf::readString);

			// Prepare client configs
			Map<String, String> clientConfigs = new HashMap<>();
			for (String filename : serverConfigs.keySet()) {
				String content = ConfigSync.getConfigContent(filename);
				if (content != null) {
					clientConfigs.put(filename, content);
				} else {
					LOGGER.warn("Failed to read client config: {}", filename);
				}
			}

			// Send client configs back to server
			PacketByteBuf response = PacketByteBufs.create();
			response.writeMap(clientConfigs, PacketByteBuf::writeString, PacketByteBuf::writeString);

			return CompletableFuture.completedFuture(response);
		});
	}
	public static ModConfig getConfig() {
		return config;
	}
}