package com.junnio.anticonfig;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.junnio.anticonfig.config.ConfigSync;
import com.junnio.anticonfig.config.ModConfig;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AnticonfigClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("ClientModInitializer");

	@Override
	public void onInitializeClient() {
		// Register config sync handler
		ClientLoginNetworking.registerGlobalReceiver(ConfigSync.CONFIG_SYNC_ID, (client, handler, buf, listenerAdder) -> {
			// Read server configs
			Map<String, String> serverConfigs = buf.readMap(PacketByteBuf::readString, PacketByteBuf::readString);
			ConfigScreenHandler.setServerConfigs(serverConfigs);
			// Prepare client configs
			Map<String, String> clientConfigs = new HashMap<>();
			Path configDir = FabricLoader.getInstance().getConfigDir();

			// Read only the configs that server requested
			for (String filename : serverConfigs.keySet()) {
				Path configPath = configDir.resolve(filename);
				try {
					FileConfig config;
					if (filename.endsWith(".json")) {
						config = FileConfig.of(configPath, JsonFormat.minimalInstance());
					} else if (filename.endsWith(".toml")) {
						config = FileConfig.of(configPath, TomlFormat.instance());
					} else {
						continue;
					}
					config.load();
					clientConfigs.put(filename, config.valueMap().toString());
				} catch (Exception e) {
					LOGGER.warn("Failed to read client config: {}", filename);
				}
			}

			// Send client configs back to server
			PacketByteBuf response = PacketByteBufs.create();
			response.writeMap(clientConfigs, PacketByteBuf::writeString, PacketByteBuf::writeString);

			return CompletableFuture.completedFuture(response);
		});
		// Screen events for config changes
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof ClothConfigScreen) {
				ScreenEvents.remove(screen).register((closedScreen) -> {
					System.out.println("Screen closed");
					ConfigScreenHandler.onConfigScreenClose();
				});
			}
		});
	}

}