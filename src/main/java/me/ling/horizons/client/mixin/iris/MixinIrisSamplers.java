package me.ling.horizons.client.mixin.iris;

import com.google.common.collect.ImmutableSet;
import me.ling.horizons.client.iris.LingSamplers;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = IrisSamplers.class, remap = false)
public class MixinIrisSamplers {
    @Inject(method = "addRenderTargetSamplers", at = @At("TAIL"))
    private static void voxy$injectSamplers(SamplerHolder samplers, Supplier<ImmutableSet<Integer>> flipped, RenderTargets renderTargets, boolean isFullscreenPass, WorldRenderingPipeline pipeline, CallbackInfo ci) {
        if (pipeline instanceof IrisRenderingPipeline ipipe) {
            LingSamplers.addSamplers(ipipe, samplers);
        }
    }
}
