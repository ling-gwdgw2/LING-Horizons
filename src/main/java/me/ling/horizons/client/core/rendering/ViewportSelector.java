package me.ling.horizons.client.core.rendering;

import me.ling.horizons.client.core.util.IrisUtil;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ViewportSelector <T extends Viewport<?>> {
    // MC 1.21.1 NeoForge: Vivecraft not available - always false
    public static final boolean VIVECRAFT_INSTALLED = ModList.get() != null && ModList.get().isLoaded("vivecraft");

    private final Supplier<T> creator;
    private final T defaultViewport;
    private final Map<Object, T> extraViewports = new HashMap<>();//TODO should maybe be a weak hashmap with value cleanup queue thing?

    public ViewportSelector(Supplier<T> viewportCreator) {
        this.creator = viewportCreator;
        this.defaultViewport = viewportCreator.get();
    }

    private T getOrCreate(Object holder) {
        return this.extraViewports.computeIfAbsent(holder, a->this.creator.get());
    }

    // MC 1.21.1 NeoForge: Vivecraft VR rendering not available
    // private T getVivecraftViewport() {
    //     var pass = VRRenderingAPI.instance().getCurrentRenderPass();
    //     if (pass == null || pass == VANILLA) {
    //         return null;
    //     }
    //     return this.getOrCreate(pass);
    // }

    private static final Object IRIS_SHADOW_OBJECT = new Object();
    public T getViewport() {
        if (IrisUtil.irisShadowActive()) {
            return this.getOrCreate(IRIS_SHADOW_OBJECT);
        }
        return this.defaultViewport;
    }

    public void free() {
        this.defaultViewport.delete();
        this.extraViewports.values().forEach(Viewport::delete);
        this.extraViewports.clear();
    }
}
