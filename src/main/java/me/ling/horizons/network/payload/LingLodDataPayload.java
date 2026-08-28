package me.ling.horizons.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload sent from Server to Client containing compressed Voxel DAG LOD data for a section.
 */
public record LingLodDataPayload(long sectionKey, int detailLevel, byte[] compressedVoxelData) implements CustomPacketPayload {
    public static final Type<LingLodDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("ling_horizons", "lod_data"));
    public static final StreamCodec<FriendlyByteBuf, LingLodDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeLong(payload.sectionKey);
                buf.writeVarInt(payload.detailLevel);
                buf.writeByteArray(payload.compressedVoxelData);
            },
            buf -> new LingLodDataPayload(buf.readLong(), buf.readVarInt(), buf.readByteArray())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
