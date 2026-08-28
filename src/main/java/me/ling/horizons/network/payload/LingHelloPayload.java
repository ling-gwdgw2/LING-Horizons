package me.ling.horizons.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Handshake payload sent when player connects to a server.
 */
public record LingHelloPayload(int protocolVersion, int maxStreamingRadiusChunks, boolean serverStreamingEnabled) implements CustomPacketPayload {
    public static final Type<LingHelloPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("ling_horizons", "hello"));
    public static final StreamCodec<FriendlyByteBuf, LingHelloPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.protocolVersion);
                buf.writeVarInt(payload.maxStreamingRadiusChunks);
                buf.writeBoolean(payload.serverStreamingEnabled);
            },
            buf -> new LingHelloPayload(buf.readVarInt(), buf.readVarInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
