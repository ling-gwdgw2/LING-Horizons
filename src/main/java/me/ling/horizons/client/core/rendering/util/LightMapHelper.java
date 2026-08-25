package me.ling.horizons.client.core.rendering.util;

import static org.lwjgl.opengl.GL33.glBindSampler;
import static org.lwjgl.opengl.GL45.glBindTextureUnit;

import me.ling.horizons.client.mixin.minecraft.AccessorLightTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;

public class LightMapHelper {
    public static void bind(int lightingIndex) {
        glBindSampler(lightingIndex, 0);
        // MC 1.21.1: Use accessor mixin to get the private DynamicTexture field, then call getId()
        LightTexture lightTexture = Minecraft.getInstance().gameRenderer.lightTexture();
        int glId = ((AccessorLightTexture) lightTexture).voxy$getLightTexture().getId();
        glBindTextureUnit(lightingIndex, glId);
    }
}