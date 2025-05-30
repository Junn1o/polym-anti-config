package com.junnio.anticonfig.net;

import com.junnio.anticonfig.Anticonfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ConfigSyncPayload(Map<String, String> configs) implements CustomPayload {
    public static final CustomPayload.Id<ConfigSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of(Anticonfig.MODID, "config_sync"));
    private static final PacketCodec<RegistryByteBuf, Map<String, String>> CONFIG_MAP_CODEC =
            PacketCodecs.map(
                    HashMap::new,
                    PacketCodecs.STRING,  // filename
                    PacketCodecs.STRING   // content
            );
    public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC = CONFIG_MAP_CODEC
            .xmap(ConfigSyncPayload::new, ConfigSyncPayload::configs);

    @Override
    public CustomPayload.Id<?> getId() {
        return ID;
    }
}