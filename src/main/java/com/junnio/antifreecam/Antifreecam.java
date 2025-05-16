package com.junnio.antifreecam;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Antifreecam implements ModInitializer {
	public static final String MODID = "antifreecam";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static final Identifier UPDATE_C2S_CHANNEL = id("update");
	public static final Identifier CONFIG_SYNC_CHANNEL = id("config_sync");
	@Override
	public void onInitialize() {
    }
	public static Identifier id(String path) {
		return Identifier.of(MODID, path);
	}
}