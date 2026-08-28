package me.ling.horizons;

import me.ling.horizons.client.config.LingNeoForgeConfig;
import me.ling.horizons.common.Logger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Main mod class for LING Horizons on NeoForge.
 *
 * Handles config registration and config screen setup.
 * Actual initialization happens via mixins (MixinRenderSystem).
 */
@Mod("ling_horizons")
public class LingHorizons {

    public LingHorizons(IEventBus modEventBus, ModContainer container) {
        Logger.info("Initializing LING Horizons (Next-Gen Voxel LOD Engine)...");

        // Only register client config on client side
        if (FMLLoader.getDist() == Dist.CLIENT) {
            // Register NeoForge config
            LingNeoForgeConfig.register(container);

            // Register the built-in NeoForge config screen
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
