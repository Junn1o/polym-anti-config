package com.junnio.anticonfig;

import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.net.NetworkManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Anticonfig implements ModInitializer {
	public static final String MODID = "anticonfig";

	@Override
	public void onInitialize() {
		ModConfig.load();
		NetworkManager.init();
	}
}