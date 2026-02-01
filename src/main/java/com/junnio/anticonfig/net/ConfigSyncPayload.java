package com.junnio.anticonfig.net;

import com.junnio.anticonfig.Anticonfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigSyncPayload(Map<String, String> configs) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfigSyncPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Anticonfig.MODID, "config_sync"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, String>> CONFIG_MAP_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    ByteBufCodecs.STRING_UTF8,  // filename
                    ByteBufCodecs.STRING_UTF8   // content
            );
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> CODEC = CONFIG_MAP_CODEC
            .map(ConfigSyncPayload::new, ConfigSyncPayload::configs);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}