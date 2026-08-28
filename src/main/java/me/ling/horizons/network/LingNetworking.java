package me.ling.horizons.network;

import me.ling.horizons.network.payload.LingHelloPayload;
import me.ling.horizons.network.payload.LingLodDataPayload;
import me.ling.horizons.network.payload.LingLodRequestPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers LING Horizons network channels and payloads on NeoForge.
 */
@EventBusSubscriber(modid = "ling_horizons", bus = EventBusSubscriber.Bus.MOD)
public class LingNetworking {
    public static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();

        registrar.playToClient(
                LingHelloPayload.TYPE,
                LingHelloPayload.STREAM_CODEC,
                LingNetworkHandler::handleHello
        );

        registrar.playToServer(
                LingLodRequestPayload.TYPE,
                LingLodRequestPayload.STREAM_CODEC,
                LingNetworkHandler::handleLodRequest
        );

        registrar.playToClient(
                LingLodDataPayload.TYPE,
                LingLodDataPayload.STREAM_CODEC,
                LingNetworkHandler::handleLodData
        );
    }
}
