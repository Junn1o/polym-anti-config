package com.junnio.antifreecam;

import com.junnio.antifreecam.config.ConfigSync;
import com.junnio.antifreecam.config.ModConfig;
import com.junnio.antifreecam.net.NetworkManager;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Antifreecam implements ModInitializer {
	public static final String MODID = "antifreecam";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	private static ModConfig config;
	@Override
	public void onInitialize() {
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		NetworkManager.init();
		ConfigSync.registerConfigsFromModConfig();
	}

	public static Identifier id(String path) {
		return Identifier.of(MODID, path);
	}
}