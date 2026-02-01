package com.junnio.anticonfig;

import com.junnio.anticonfig.command.ModCommand;
import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.net.ModNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class Anticonfig implements ModInitializer {
	public static final String MODID = "anticonfig";

	@Override
	public void onInitialize() {
		ModConfig.init();
		ModNetwork.init("1.0.1");
		ModCommand.init();
	}
}