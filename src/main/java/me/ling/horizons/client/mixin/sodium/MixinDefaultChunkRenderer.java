package me.ling.horizons.client.mixin.sodium;

import me.ling.horizons.client.LingClient;
import me.ling.horizons.client.core.IGetLingRenderSystem;
import me.ling.horizons.client.core.rendering.Viewport;
import me.ling.horizons.client.core.util.IrisUtil;
import me.ling.horizons.commonImpl.LingCommon;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class MixinDefaultChunkRenderer extends ShaderChunkRenderer {

    public MixinDefaultChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
    }

    // Sodium 0.6.13: (ChunkRenderMatrices, CommandList, ChunkRenderListIterable, TerrainRenderPass, CameraTransform)
    @Inject(method = "render(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;)V", at = @At(value = "HEAD"), cancellable = true, require = 0)
    private void cancelThingie_legacy(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform camera, CallbackInfo ci) {
        if (IrisUtil.irisShadowActive()) {
            return;
        }
        if (LingClient.disableSodiumChunkRender()) {
            super.begin(renderPass);
            this.doRender(matrices, renderPass, camera);
            super.end(renderPass);
            ci.cancel();
        }
    }

    // Sodium 0.8.12+: (ChunkRenderMatrices, CommandList, ChunkRenderListIterable, TerrainRenderPass, CameraTransform, boolean)
    @Inject(method = "render(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Z)V", at = @At(value = "HEAD"), cancellable = true, require = 0)
    private void cancelThingie_v08(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform camera, boolean flag, CallbackInfo ci) {
        if (IrisUtil.irisShadowActive()) {
            return;
        }
        if (LingClient.disableSodiumChunkRender()) {
            super.begin(renderPass);
            this.doRender(matrices, renderPass, camera);
            super.end(renderPass);
            ci.cancel();
        }
    }

    // Sodium 0.6.13: (ChunkRenderMatrices, CommandList, ChunkRenderListIterable, TerrainRenderPass, CameraTransform)
    @Inject(method = "render(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;)V", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/ShaderChunkRenderer;end(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V", shift = At.Shift.BEFORE), require = 0)
    private void injectRender_legacy(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform camera, CallbackInfo ci) {
        this.doRender(matrices, renderPass, camera);
    }

    // Sodium 0.8.12+: (ChunkRenderMatrices, CommandList, ChunkRenderListIterable, TerrainRenderPass, CameraTransform, boolean)
    @Inject(method = "render(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Z)V", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/ShaderChunkRenderer;end(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V", shift = At.Shift.BEFORE), require = 0)
    private void injectRender_v08(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform camera, boolean flag, CallbackInfo ci) {
        this.doRender(matrices, renderPass, camera);
    }

    @Unique
    private void doRender(ChunkRenderMatrices matrices, TerrainRenderPass renderPass, CameraTransform camera) {
        if (IrisUtil.irisShadowActive()) {
            return;
        }
        if (renderPass == DefaultTerrainRenderPasses.CUTOUT) {
            var renderer = ((IGetLingRenderSystem) Minecraft.getInstance().levelRenderer).getLingRenderSystem();
            if (renderer != null) {
                Viewport<?> viewport = null;
                if (IrisUtil.irisShaderPackEnabled()) {
                    viewport = renderer.getViewport();
                } else {
                    // Sodium 0.6.x: setupViewport no longer takes FogParameters
                    viewport = renderer.setupViewport(matrices, camera.x, camera.y, camera.z);
                }
                renderer.renderOpaque(viewport);
            }
        }
    }
}
