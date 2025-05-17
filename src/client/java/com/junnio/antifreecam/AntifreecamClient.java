package com.junnio.antifreecam;

import com.junnio.antifreecam.config.ConfigSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AntifreecamClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register configs to check
		ConfigSync.registerConfigToCheck("freecam.json");
		ConfigSync.registerConfigToCheck("leavemybarsalone-client.toml");

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
				}
			}

			// Send client configs back to server
			PacketByteBuf response = PacketByteBufs.create();
			response.writeMap(clientConfigs, PacketByteBuf::writeString, PacketByteBuf::writeString);

			return CompletableFuture.completedFuture(response);
		});
	}
}