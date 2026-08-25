package me.ling.horizons.client.mixin.minecraft;

import me.ling.horizons.client.LingClientInstance;
import me.ling.horizons.client.config.LingConfig;
import me.ling.horizons.client.core.IGetLingRenderSystem;
import me.ling.horizons.client.core.LingRenderSystem;
// MC 1.21.1 NeoForge: Iris shader integration excluded
// import me.ling.horizons.client.core.util.IrisUtil;
import me.ling.horizons.common.Logger;
import me.ling.horizons.common.world.WorldEngine;
import me.ling.horizons.commonImpl.LingCommon;
import me.ling.horizons.commonImpl.WorldIdentifier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer implements IGetLingRenderSystem {
    @Shadow private @Nullable ClientLevel level;
    @Unique private LingRenderSystem renderer;

    @Override
    public LingRenderSystem getLingRenderSystem() {
        return this.renderer;
    }

    @Inject(method = "allChanged()V", at = @At("RETURN"), order = 900)//We want to inject before sodium
    private void reloadVoxyRenderer(CallbackInfo ci) {
        this.shutdownRenderer();
        if (this.level != null) {
            this.createRenderer();
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void voxy$captureSetWorld(ClientLevel world, CallbackInfo ci) {
        me.ling.horizons.client.pregen.WorldPregenerator.getInstance().cancelPregen();
        if (this.level != world) {
            this.shutdownRenderer();
        }
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void voxy$onSetWorldReturn(ClientLevel world, CallbackInfo ci) {
        if (world != null && this.renderer == null) {
            this.createRenderer();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void injectClose(CallbackInfo ci) {
        me.ling.horizons.client.pregen.WorldPregenerator.getInstance().cancelPregen();
        this.shutdownRenderer();
    }

    @Override
    public void shutdownRenderer() {
        if (this.renderer != null) {
            this.renderer.shutdown();
            this.renderer = null;
        }
    }

    @Override
    public void createRenderer() {
        if (this.renderer != null) throw new IllegalStateException("Cannot have multiple renderers");
        if (!LingConfig.CONFIG.enabled) {
            Logger.info("Not creating renderer due to disabled");
            return;
        }
        if (!LingConfig.CONFIG.isRenderingEnabled()) {
            Logger.info("Not creating renderer due to disabled rendering");
            return;
        }
        if (this.level == null) {
            Logger.error("Not creating renderer due to null world");
            return;
        }
        var instance = (LingClientInstance)LingCommon.getInstance();
        if (instance == null) {
            Logger.error("Not creating renderer due to null instance");
            return;
        }
        WorldEngine world = WorldIdentifier.ofEngine(this.level);
        if (world == null) {
            Logger.error("Null world selected");
            return;
        }
        try {
            this.renderer = new LingRenderSystem(world, instance.getServiceManager());
        } catch (RuntimeException e) {
            // MC 1.21.1 NeoForge: Iris shader integration excluded - irisShaderPackEnabled() returns false
            if (false) {
                // IrisUtil.disableIrisShaders();
            } else {
                throw e;
            }
        }
        instance.updateDedicatedThreads();
    }
}
