package com.junnio.anticonfig.client.net;

import com.junnio.anticonfig.net.VersionCheckPayLoad;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModNetwork {
    public static void init(String clientver) {
        ClientLoginNetworking.registerGlobalReceiver(com.junnio.anticonfig.net.ModNetwork.CONFIG_SYNC_ID, (client, handler, buf, listenerAdder) -> {

            Map<String, String> serverConfigs = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);
            ConfigSyncHelper.setServerConfigs(serverConfigs);

            Map<String, String> clientConfigs = ConfigSyncHelper.readConfigsForSync(serverConfigs.keySet());
            return CompletableFuture.completedFuture(ConfigSyncHelper.createConfigSyncPacket(clientConfigs));
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientPlayNetworking.send(new VersionCheckPayLoad(clientver));
        });
    }
}
