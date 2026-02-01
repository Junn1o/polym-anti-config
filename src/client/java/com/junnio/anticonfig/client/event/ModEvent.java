package com.junnio.anticonfig.client.event;

import com.junnio.anticonfig.client.net.ConfigSyncHelper;
import com.terraformersmc.modmenu.gui.ModsScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class ModEvent {
    public static void init() {
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
