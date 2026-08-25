package me.ling.horizons.client.mixin.iris;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.shaderpack.include.ShaderPackSourceNames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ShaderPackSourceNames.class, remap = false)
public class MixinShaderPackSourceNames {
    @WrapOperation(method = "findPotentialStarts", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;"))
    private static ImmutableList.Builder<String> voxy$injectVoxyShaderPatch(Operation<ImmutableList.Builder<String>> original){
        var builder = original.call();
        // External shaderpack compatibility starts (Photon, Complementary, Bliss)
        builder.add("voxy.json");
        builder.add("voxy_opaque.glsl");
        builder.add("voxy_translucent.glsl");
        // Native LING Horizons shaderpatch starts
        builder.add("ling_horizons.json");
        builder.add("ling_horizons_opaque.glsl");
        builder.add("ling_horizons_translucent.glsl");
        return builder;
    }
}
