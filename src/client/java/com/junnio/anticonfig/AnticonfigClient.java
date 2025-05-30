package com.junnio.anticonfig;

import com.junnio.anticonfig.net.NetworkManager;
import com.junnio.anticonfig.net.ConfigSyncHelper;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.network.PacketByteBuf;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AnticonfigClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLoginNetworking.registerGlobalReceiver(NetworkManager.CONFIG_SYNC_ID, (client, handler, buf, listenerAdder) -> {
            Map<String, String> serverConfigs = buf.readMap(PacketByteBuf::readString, PacketByteBuf::readString);
            ConfigSyncHelper.setServerConfigs(serverConfigs);

            Map<String, String> clientConfigs = ConfigSyncHelper.readConfigsForSync(serverConfigs.keySet());
            return CompletableFuture.completedFuture(ConfigSyncHelper.createConfigSyncPacket(clientConfigs));
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ClothConfigScreen) {
                ScreenEvents.remove(screen).register((closedScreen) -> ConfigSyncHelper.onConfigScreenClose());
            }
        });
    }
}