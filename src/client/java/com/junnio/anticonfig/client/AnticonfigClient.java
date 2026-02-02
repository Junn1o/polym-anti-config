package com.junnio.anticonfig.client;

import com.junnio.anticonfig.client.event.ModEvent;
import com.junnio.anticonfig.client.net.ModNetwork;
import net.fabricmc.api.ClientModInitializer;
public class AnticonfigClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModNetwork.init();
        ModEvent.init();
    }
}