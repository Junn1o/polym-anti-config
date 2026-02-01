package com.junnio.anticonfig.net;


import com.junnio.anticonfig.Anticonfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VersionCheckPayLoad(String ver) implements CustomPacketPayload {
    public static final Identifier version = Identifier.fromNamespaceAndPath(Anticonfig.MODID, "version_check");
    public static final CustomPacketPayload.Type<VersionCheckPayLoad> ID = new CustomPacketPayload.Type<>(version);
    public static final StreamCodec<RegistryFriendlyByteBuf, VersionCheckPayLoad> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, VersionCheckPayLoad::ver,
                    VersionCheckPayLoad::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}