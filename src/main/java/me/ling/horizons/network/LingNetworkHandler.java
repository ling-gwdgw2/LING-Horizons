package me.ling.horizons.network;

import me.ling.horizons.client.network.ClientLodStreamManager;
import me.ling.horizons.common.Logger;
import me.ling.horizons.network.payload.LingHelloPayload;
import me.ling.horizons.network.payload.LingLodDataPayload;
import me.ling.horizons.network.payload.LingLodRequestPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Dispatches and processes incoming LING Horizons network payloads on client.
 */
public class LingNetworkHandler {

    public static void handleHello(LingHelloPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Logger.info("Received LING Horizons Server Hello: protocol=" + payload.protocolVersion() +
                    ", streamingRadius=" + payload.maxStreamingRadiusChunks() +
                    ", enabled=" + payload.serverStreamingEnabled());
            ClientLodStreamManager.onServerHello(payload);
        });
    }

    public static void handleLodRequest(LingLodRequestPayload payload, IPayloadContext context) {
        // Handled on dedicated server mod
    }

    public static void handleLodData(LingLodDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLodStreamManager.onLodDataReceived(payload);
        });
    }
}
