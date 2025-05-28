package com.junnio.anticonfig;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.hocon.HoconFormat;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.junnio.anticonfig.config.ModConfig;
import com.junnio.anticonfig.net.parser.*;
import com.junnio.anticonfig.net.NetworkManager;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public class AnticonfigClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("ClientModInitializer");

    @Override
    public void onInitializeClient() {
        // Register config sync handler
        ClientLoginNetworking.registerGlobalReceiver(NetworkManager.CONFIG_SYNC_ID, (client, handler, buf, listenerAdder) -> {
            // Read server configs
            Map<String, String> serverConfigs = buf.readMap(PacketByteBuf::readString, PacketByteBuf::readString);
            ConfigScreenHandler.setServerConfigs(serverConfigs);
            // Prepare client configs
            Map<String, String> clientConfigs = new HashMap<>();
            Path configDir = FabricLoader.getInstance().getConfigDir();

            // Read only the configs that server requested
            for (String filename : serverConfigs.keySet()) {
                Path configPath = ModConfig.resolveConfigPath(filename);
                if (Files.exists(configPath)) {
                    String clientContent;
                    try {
                        if (filename.endsWith(".json")) {
                            FileConfig fileConfig = FileConfig.of(configPath, JsonFormat.fancyInstance());
                            fileConfig.load();
                            clientContent = NightConfigParser.configToString(fileConfig);
                        } else if (filename.endsWith(".toml")) {
                            FileConfig fileConfig = FileConfig.of(configPath, TomlFormat.instance());
                            fileConfig.load();
                            clientContent = NightConfigParser.configToString(fileConfig);
                        } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                            FileConfig fileConfig = FileConfig.of(configPath, YamlFormat.defaultInstance());
                            fileConfig.load();
                            clientContent = NightConfigParser.configToString(fileConfig);
                        } else if (filename.endsWith(".json5")) {
                            clientContent = Json5Parser.json5ToString(configPath);
                        } else if (filename.endsWith(".hocon")) {
                            FileConfig fileConfig = FileConfig.of(configPath, HoconFormat.instance());
                            fileConfig.load();
                            clientContent = NightConfigParser.configToString(fileConfig);
                        } else if (filename.endsWith(".ini")) {
                            clientContent = IniParser.iniToString(configPath);
                        } else if (filename.endsWith(".properties") || filename.endsWith(".conf") || filename.endsWith(".cfg")) {
                            clientContent = PropertiesParser.propertiesToString(configPath);
                        } else if (filename.endsWith(".txt")) {
                            clientContent = TxtConfigParser.txtToString(configPath);
                        } else {
                            continue;
                        }
                        System.out.println("Client config: " + filename + " = " + clientContent);
                        clientConfigs.put(filename, clientContent);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to read client config: {}", filename);
                    }
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