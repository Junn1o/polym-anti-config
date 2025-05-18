package com.junnio.antifreecam.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import com.junnio.antifreecam.Antifreecam;

import java.util.Map;

public class ConfigVerificationPacket {
    // Server -> Client: Send server's config values
    public static final Identifier SEND_CONFIG = Identifier.of("antifreecam", "send_config");

    // Client -> Server: Request config values
    public static final Identifier REQUEST_CONFIG = Identifier.of("antifreecam", "request_config");

}