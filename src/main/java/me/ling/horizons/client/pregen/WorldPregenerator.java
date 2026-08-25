package me.ling.horizons.client.pregen;

import me.ling.horizons.client.config.LingConfig;
import me.ling.horizons.common.Logger;
import me.ling.horizons.common.world.WorldEngine;
import me.ling.horizons.common.world.service.VoxelIngestService;
import me.ling.horizons.commonImpl.WorldIdentifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * World Pre-generator for Voxy LODs.
 * Generates and ingests chunks in a circular radius centered on the player (Center-Out Radial / Spiral Order)
 * using true multi-threaded background worker threads with smart-skip of existing chunks and real-time HUD progress.
 */
public class WorldPregenerator {
    private static final WorldPregenerator INSTANCE = new WorldPregenerator();

    public static WorldPregenerator getInstance() {
        return INSTANCE;
    }

    private record ChunkOffset(int dx, int dz, int distSq) {}

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger processedChunks = new AtomicInteger(0);
    private final AtomicInteger skippedChunks = new AtomicInteger(0);
    private final AtomicInteger totalChunks = new AtomicInteger(0);
    private ExecutorService pregenExecutor;
    private long startTime;

    public synchronized boolean startPregen(int radiusChunks) {
        // If already running, cancel previous run first to restart cleanly
        if (this.isRunning.get()) {
            this.cancelPregen();
        }

        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }

        var server = mc.getSingleplayerServer();
        if (server == null) {
            if (mc.gui != null && mc.gui.getChat() != null) {
                mc.gui.getChat().addMessage(Component.literal("[Voxy Pregen] Pre-generation is currently available in Singleplayer / Integrated Server worlds.").withStyle(ChatFormatting.YELLOW));
            }
            return false;
        }

        ServerLevel serverLevel = server.getLevel(mc.level.dimension());
        if (serverLevel == null) {
            return false;
        }

        int centerChunkX = mc.player.getBlockX() >> 4;
        int centerChunkZ = mc.player.getBlockZ() >> 4;

        // Build list of circular chunk offsets centered on the player
        List<ChunkOffset> offsets = new ArrayList<>();
        int radiusSq = radiusChunks * radiusChunks;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq <= radiusSq) {
                    offsets.add(new ChunkOffset(dx, dz, distSq));
                }
            }
        }
        // Sort from closest to player (center) outwards to horizon
        offsets.sort(Comparator.comparingInt(ChunkOffset::distSq));

        int total = offsets.size();
        this.totalChunks.set(total);
        this.processedChunks.set(0);
        this.skippedChunks.set(0);
        this.startTime = System.currentTimeMillis();
        this.isRunning.set(true);

        int threads = Math.max(1, Math.min(4, LingConfig.CONFIG.autoPregenThreads));
        this.pregenExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "Voxy-WorldPregenerator");
            t.setDaemon(true);
            return t;
        });

        if (mc.gui != null && mc.gui.getChat() != null) {
            mc.gui.getChat().addMessage(Component.literal(String.format("[Voxy Pregen] Starting circular LOD generation in %d chunks radius (%d total chunks with %d worker threads)...", radiusChunks, total, threads)).withStyle(ChatFormatting.GREEN));
        }

        AtomicInteger offsetIndex = new AtomicInteger(0);
        AtomicLong lastHudUpdate = new AtomicLong(System.currentTimeMillis());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            futures.add(CompletableFuture.runAsync(() -> {
                WorldEngine engine = WorldIdentifier.ofEngineNullable(mc.level);
                int minY = mc.level != null ? mc.level.getMinBuildHeight() >> 4 : -4;
                int maxY = mc.level != null ? mc.level.getMaxBuildHeight() >> 4 : 20;

                while (this.isRunning.get()) {
                    if (Thread.currentThread().isInterrupted() || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null || !mc.getSingleplayerServer().isRunning()) {
                        this.isRunning.set(false);
                        break;
                    }

                    int idx = offsetIndex.getAndIncrement();
                    if (idx >= offsets.size()) {
                        break;
                    }

                    var offset = offsets.get(idx);
                    int cx = centerChunkX + offset.dx;
                    int cz = centerChunkZ + offset.dz;

                    // Smart-Skip check: Check if chunk already exists in Voxy's database
                    boolean alreadyIngested = false;
                    if (engine != null) {
                        for (int y = minY; y < maxY; y += 4) {
                            var sec = engine.acquireIfExists(0, cx, y, cz);
                            if (sec != null) {
                                sec.release();
                                alreadyIngested = true;
                                break;
                            }
                        }
                    }

                    if (alreadyIngested) {
                        this.skippedChunks.incrementAndGet();
                        this.processedChunks.incrementAndGet();
                        continue;
                    }

                    try {
                        // Generate chunk from server chunk source on worker thread
                        var chunk = serverLevel.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, true);
                        if (chunk instanceof LevelChunk levelChunk) {
                            VoxelIngestService.tryAutoIngestChunk(levelChunk);
                        }
                    } catch (Exception e) {
                        if (this.isRunning.get()) {
                            Logger.error("Failed to generate chunk at " + cx + ", " + cz, e);
                        }
                    }

                    int current = this.processedChunks.incrementAndGet();
                    long now = System.currentTimeMillis();
                    long last = lastHudUpdate.get();

                    if (now - last >= 250 && lastHudUpdate.compareAndSet(last, now)) {
                        double elapsedSec = Math.max(0.1, (now - this.startTime) / 1000.0);
                        double rate = current / elapsedSec;
                        int percent = (int) ((current / (double) total) * 100.0);

                        mc.execute(() -> {
                            if (this.isRunning.get() && mc.player != null) {
                                mc.gui.setOverlayMessage(Component.literal(
                                        String.format("[Voxy Pregen] Progress: %d%% (%d / %d chunks, %d skipped) - %.1f chunks/s", percent, current, total, this.skippedChunks.get(), rate)
                                ).withStyle(ChatFormatting.AQUA), false);
                            }
                        });
                    }
                }
            }, this.pregenExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((res, err) -> {
            long totalElapsed = System.currentTimeMillis() - this.startTime;
            double seconds = totalElapsed / 1000.0;
            int finished = this.processedChunks.get();
            int skipped = this.skippedChunks.get();

            mc.execute(() -> {
                if (mc.gui != null && mc.gui.getChat() != null && mc.player != null) {
                    if (this.isRunning.get()) {
                        mc.gui.getChat().addMessage(Component.literal(
                                String.format("[Voxy Pregen] Finished! Processed %d chunks (%d skipped) in %.2f seconds.", finished, skipped, seconds)
                        ).withStyle(ChatFormatting.GREEN));
                    }
                }
            });
            this.isRunning.set(false);
        });

        return true;
    }

    public synchronized boolean cancelPregen() {
        if (this.isRunning.compareAndSet(true, false)) {
            if (this.pregenExecutor != null) {
                this.pregenExecutor.shutdownNow();
                this.pregenExecutor = null;
            }
            var mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.gui != null) {
                    mc.gui.setOverlayMessage(Component.empty(), false);
                    if (mc.gui.getChat() != null && mc.player != null) {
                        mc.gui.getChat().addMessage(Component.literal("[Voxy Pregen] Pre-generation stopped.").withStyle(ChatFormatting.YELLOW));
                    }
                }
            });
            return true;
        }
        return false;
    }

    public boolean isRunning() {
        return this.isRunning.get();
    }

    public static void triggerAutoPregenIfEnabled() {
        if (LingConfig.CONFIG.enabled && LingConfig.CONFIG.autoPregenOnJoin) {
            CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
                var mc = Minecraft.getInstance();
                if (mc.player != null && mc.getSingleplayerServer() != null && !INSTANCE.isRunning()) {
                    INSTANCE.startPregen(LingConfig.CONFIG.autoPregenRadius);
                }
            });
        }
    }
}
