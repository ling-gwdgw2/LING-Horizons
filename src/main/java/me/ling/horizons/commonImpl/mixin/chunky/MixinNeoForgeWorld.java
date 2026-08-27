package me.ling.horizons.commonImpl.mixin.chunky;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.ling.horizons.common.world.service.VoxelIngestService;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

/**
 * Chunky NeoForge Integration for LING Horizons.
 * Intercepts chunks pre-generated via /chunky and automatically ingests them
 * into the 3D Voxel LOD database in real time.
 */
@Pseudo
@Mixin(targets = "org.popcraft.chunky.platform.NeoForgeWorld", remap = false)
public class MixinNeoForgeWorld {
    @WrapOperation(
            method = "getChunkAtAsync",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerChunkCache;getChunkFutureMainThread(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Ljava/util/concurrent/CompletableFuture;",
                    remap = true
            ),
            require = 0
    )
    private CompletableFuture<ChunkResult<ChunkAccess>> voxy$captureGeneratedChunk(
            ServerChunkCache instance,
            int x,
            int z,
            ChunkStatus chunkStatus,
            boolean create,
            Operation<CompletableFuture<ChunkResult<ChunkAccess>>> original
    ) {
        var future = original.call(instance, x, z, chunkStatus, create);
        return future.thenApply(res -> {
            if (res != null) {
                res.ifSuccess(chunk -> {
                    if (chunk instanceof LevelChunk worldChunk) {
                        VoxelIngestService.tryAutoIngestChunk(worldChunk);
                    }
                });
            }
            return res;
        });
    }
}
