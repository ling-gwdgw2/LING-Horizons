package me.ling.horizons.client.mixin.minecraft;


// MC 1.21.1: ShaderSource and ShaderType classes moved/removed
// import com.mojang.blaze3d.shaders.ShaderSource;
// import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ling.horizons.client.LingClient;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

//Thanks iris for making me need todo this ;-; _irritater_
@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem {
    //We need to inject before iris to initalize our systems
    // MC 1.21.1: initRenderer signature: (int debugVerbosity, boolean synchronous)
    // Verified against NeoForge 21.1.217 decompiled sources
    @Inject(method = "initRenderer", order = 900, remap = false, at = @At("RETURN"))
    private static void voxy$injectInit(int debugVerbosity, boolean synchronous, CallbackInfo ci) {
        LingClient.initLingClient();
    }
}
