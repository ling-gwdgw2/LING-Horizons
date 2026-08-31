package me.ling.horizons.client.network;

import me.ling.horizons.common.Logger;
import me.ling.horizons.common.util.MemoryBuffer;
import me.ling.horizons.common.world.SaveLoadSystem3;
import me.ling.horizons.common.world.WorldEngine;
import me.ling.horizons.common.world.WorldSection;
import me.ling.horizons.commonImpl.WorldIdentifier;
import me.ling.horizons.network.payload.LingHelloPayload;
import me.ling.horizons.network.payload.LingLodDataPayload;
import me.ling.horizons.network.payload.LingLodRequestPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.system.MemoryUtil;

/**
 * Manages client-side LOD streaming requests and received voxel sections.
 */
public class ClientLodStreamManager {
    private static volatile boolean serverStreamingSupported = false;
    private static volatile int serverMaxRadiusChunks = 128;
    private static long lastRequestTime = 0;
    private static int lastRequestedX = Integer.MIN_VALUE;
    private static int lastRequestedZ = Integer.MIN_VALUE;

    public static void onServerHello(LingHelloPayload payload) {
        serverStreamingSupported = payload.serverStreamingEnabled();
        serverMaxRadiusChunks = payload.maxStreamingRadiusChunks();
        Logger.info("Server LOD Streaming is " + (serverStreamingSupported ? "ENABLED (MaxRadius=" + serverMaxRadiusChunks + " chunks)" : "DISABLED"));

        // Trigger initial request around player
        triggerRequestIfMoved(true);
    }

    public static void tick() {
        if (!serverStreamingSupported || Minecraft.getInstance().player == null) {
            return;
        }
        triggerRequestIfMoved(false);
    }

    private static void triggerRequestIfMoved(boolean force) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        int chunkX = player.getBlockX() >> 4;
        int chunkZ = player.getBlockZ() >> 4;

        long now = System.currentTimeMillis();
        // Request update if player moved more than 8 chunks or every 10 seconds
        if (force || now - lastRequestTime > 10000 || Math.abs(chunkX - lastRequestedX) >= 8 || Math.abs(chunkZ - lastRequestedZ) >= 8) {
            lastRequestTime = now;
            lastRequestedX = chunkX;
            lastRequestedZ = chunkZ;

            int requestRadius = Math.min(serverMaxRadiusChunks, 128);
            PacketDistributor.sendToServer(new LingLodRequestPayload(
                    chunkX,
                    chunkZ,
                    requestRadius,
                    0,
                    4
            ));
        }
    }

    private static final java.util.concurrent.atomic.AtomicInteger receivedSectionsCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private static long lastLogReceivedTime = 0;
    private static final java.util.concurrent.ExecutorService streamProcessingExecutor = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "LING Client LOD Stream Receiver");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    public static void onLodDataReceived(LingLodDataPayload payload) {
        streamProcessingExecutor.submit(() -> {
            processLodData(payload);
        });
    }

    private static void processLodData(LingLodDataPayload payload) {
        var level = Minecraft.getInstance().level;
        byte[] bytes = payload.compressedVoxelData();
        if (level == null || bytes == null || bytes.length == 0) {
            return;
        }

        WorldEngine engine = WorldIdentifier.ofEngine(level);
        if (engine == null) {
            return;
        }

        MemoryBuffer buffer = null;
        try {
            buffer = new MemoryBuffer(bytes.length);
            MemoryUtil.memByteBuffer(buffer.address, bytes.length).put(bytes);

            WorldSection section = engine.acquire(payload.sectionKey());
            if (section != null) {
                try {
                    boolean success = SaveLoadSystem3.deserialize(section, buffer);
                    if (success) {
                        // Mark as updated and save to client local RocksDB cache
                        engine.markDirty(section);
                        engine.storage.saveSection(section);

                        int count = receivedSectionsCount.incrementAndGet();
                        long now = System.currentTimeMillis();
                        if (now - lastLogReceivedTime > 3000) {
                            lastLogReceivedTime = now;
                            Logger.info("[LING Horizons Client] Received " + count + " streamed LOD sections from server");
                        }
                    }
                } finally {
                    section.release();
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to process streamed LOD section: " + payload.sectionKey(), e);
        } finally {
            if (buffer != null) {
                buffer.free();
            }
        }
    }

    public static void reset() {
        serverStreamingSupported = false;
        lastRequestTime = 0;
        lastRequestedX = Integer.MIN_VALUE;
        lastRequestedZ = Integer.MIN_VALUE;
    }
}
