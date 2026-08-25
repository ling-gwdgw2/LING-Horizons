package me.ling.horizons.client.mixin.minecraft;

import me.ling.horizons.client.ICheekyClientChunkCache;
import me.ling.horizons.client.config.LingConfig;
import me.ling.horizons.common.world.service.VoxelIngestService;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkCache.class)
public class MixinClientChunkCache implements ICheekyClientChunkCache {
    @Unique
    private static final boolean BOBBY_INSTALLED = ModList.get().isLoaded("bobby");

    @Shadow volatile ClientChunkCache.Storage storage;

    @Override
    public LevelChunk voxy$cheekyGetChunk(int x, int z) {
        //This doesnt do the in range check stuff, it just gets the chunk at all costs
        return this.storage.getChunk(this.storage.getIndex(x, z));
    }

    @Inject(method = "drop", at = @At("HEAD"))
    public void voxy$captureChunkBeforeUnload(ChunkPos pos, CallbackInfo ci) {
        if (LingConfig.CONFIG.ingestEnabled) {
            var chunk = this.voxy$cheekyGetChunk(pos.x, pos.z);
            if (chunk != null) {
                VoxelIngestService.tryAutoIngestChunk(chunk);
            }
        }
    }
}
