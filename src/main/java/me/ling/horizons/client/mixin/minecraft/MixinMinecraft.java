package me.ling.horizons.client.mixin.minecraft;

import me.ling.horizons.client.LingClientInstance;
import me.ling.horizons.client.pregen.WorldPregenerator;
import me.ling.horizons.commonImpl.LingCommon;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void voxy$injectWorldClose(CallbackInfo ci) {
        WorldPregenerator.getInstance().cancelPregen();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && me.ling.horizons.client.config.LingConfig.CONFIG.ingestEnabled) {
            var chunkSource = mc.level.getChunkSource();
            if (chunkSource != null) {
                int px = (mc.player != null) ? (mc.player.getBlockX() >> 4) : 0;
                int pz = (mc.player != null) ? (mc.player.getBlockZ() >> 4) : 0;
                int rd = mc.options.getEffectiveRenderDistance() + 2;
                for (int cx = px - rd; cx <= px + rd; cx++) {
                    for (int cz = pz - rd; cz <= pz + rd; cz++) {
                        var chunk = chunkSource.getChunk(cx, cz, false);
                        if (chunk != null) {
                            me.ling.horizons.common.world.service.VoxelIngestService.tryAutoIngestChunk(chunk);
                        }
                    }
                }
            }
        }
        if (mc.levelRenderer != null) {
            ((me.ling.horizons.client.core.IGetLingRenderSystem) mc.levelRenderer).shutdownRenderer();
        }
        if (LingCommon.isAvailable() && LingClientInstance.isInGame) {
            LingCommon.shutdownInstance();
            LingClientInstance.isInGame = false;
        }
    }

    /*
    @Inject(method = "joinWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setWorld(Lnet/minecraft/client/world/ClientWorld;)V", shift = At.Shift.BEFORE))
    private void voxy$injectInitialization(ClientWorld world, DownloadingTerrainScreen.WorldEntryReason worldEntryReason, CallbackInfo ci) {
        if (LingConfig.CONFIG.enabled) {
            LingCommon.createInstance();
        }
    }*/
}
