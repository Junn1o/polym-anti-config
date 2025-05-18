package com.junnio.antifreecam;

import com.junnio.antifreecam.config.ConfigSync;
import com.junnio.antifreecam.config.ModConfig;
import com.junnio.antifreecam.net.NetworkManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Antifreecam implements ModInitializer {
	public static final String MODID = "antifreecam";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitialize() {
		ModConfig.load();
		NetworkManager.init();
		ConfigSync.registerConfigsFromModConfig();
	}

	public static Identifier id(String path) {
		return Identifier.of(MODID, path);
	}
}