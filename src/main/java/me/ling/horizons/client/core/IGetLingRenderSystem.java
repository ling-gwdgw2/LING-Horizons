package me.ling.horizons.client.core;

import net.minecraft.client.Minecraft;

public interface IGetLingRenderSystem {
    LingRenderSystem getLingRenderSystem();
    void shutdownRenderer();
    void createRenderer();

    static LingRenderSystem getNullable() {
        var lr = (IGetLingRenderSystem)Minecraft.getInstance().levelRenderer;
        if (lr == null) return null;
        return lr.getLingRenderSystem();
    }
}
