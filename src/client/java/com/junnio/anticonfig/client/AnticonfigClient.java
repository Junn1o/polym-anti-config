package com.junnio.anticonfig.client;

import com.junnio.anticonfig.client.net.ConfigSyncHelper;
import com.junnio.anticonfig.net.NetworkManager;
import com.terraformersmc.modmenu.gui.ModsScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.network.FriendlyByteBuf;
public class AnticonfigClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLoginNetworking.registerGlobalReceiver(NetworkManager.CONFIG_SYNC_ID, (client, handler, buf, listenerAdder) -> {

            Map<String, String> serverConfigs = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);
            ConfigSyncHelper.setServerConfigs(serverConfigs);

            Map<String, String> clientConfigs = ConfigSyncHelper.readConfigsForSync(serverConfigs.keySet());
            return CompletableFuture.completedFuture(ConfigSyncHelper.createConfigSyncPacket(clientConfigs));
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ClothConfigScreen) {
                ScreenEvents.remove(screen).register((closedScreen) -> ConfigSyncHelper.onConfigScreenClose());
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ModsScreen) {
                ScreenEvents.remove(screen).register((closedScreen) -> ConfigSyncHelper.onConfigScreenClose());
            }
        });
    }
}