package me.ling.horizons.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload sent from Client to Server requesting LOD sections around player coordinates.
 */
public record LingLodRequestPayload(int centerChunkX, int centerChunkZ, int radiusChunks, int minDetailLevel, int maxDetailLevel) implements CustomPacketPayload {
    public static final Type<LingLodRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("ling_horizons", "lod_request"));
    public static final StreamCodec<FriendlyByteBuf, LingLodRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.centerChunkX);
                buf.writeVarInt(payload.centerChunkZ);
                buf.writeVarInt(payload.radiusChunks);
                buf.writeVarInt(payload.minDetailLevel);
                buf.writeVarInt(payload.maxDetailLevel);
            },
            buf -> new LingLodRequestPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
