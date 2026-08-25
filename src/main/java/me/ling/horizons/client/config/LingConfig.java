package me.ling.horizons.client.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.ling.horizons.common.Logger;
import me.ling.horizons.common.util.cpu.CpuLayout;
import me.ling.horizons.commonImpl.LingCommon;
import net.neoforged.fml.loading.FMLPaths;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

public class LingConfig {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static LingConfig CONFIG = loadOrCreate();

    public boolean enabled = true;
    public boolean enableRendering = true;
    public boolean ingestEnabled = true;
    public int sectionRenderDistance = 16;
    public int serviceThreads = (int) Math.max(CpuLayout.getCoreCount()/1.5, 1);
    public float subDivisionSize = 96.0f;
    public boolean useEnvironmentalFog = true;
    public boolean dontUseSodiumBuilderThreads = false;

    // LOD boundary buffer: controls the safety margin between vanilla chunks and LOD rendering
    // Higher values = more overlap, prevents pop-in at chunk boundaries when flying
    // Range: 0-4 blocks, default 1 (original Voxy behavior)
    public int lodBoundaryBuffer = 1;

    // World curvature: simulates standing on a spherical planet
    // 0 = disabled (flat world)
    // 1 = real Earth curvature (6371km radius)
    // Higher values = more extreme curvature (smaller planet effect)
    // Range: 0, or 50-5000 (values 1-49 are invalid and auto-corrected to 50)
    // Inspired by Distant Horizons' earth curvature feature
    public int earthCurveRatio = 0;

    // Geometry VRAM buffer size in MB (0 = Auto detection based on GPU VRAM, e.g. 512, 768, 1024, 1536)
    // Prevents VRAM overflow on 4GB / 6GB / 8GB GPUs
    public int geometryBufferSizeMB = 0;

    // LOD Water & Translucent Reflection (Screen-Space Reflection & wave normal enhancement)
    public boolean enableWaterSSR = true;

    // Distant Shader Shadows & Lighting (Cloud shadows, sunlight attenuation on LODs via Iris)
    // Can be disabled to save performance on low-end GPUs
    public boolean enableDistantShaderShadows = true;

    // World Pre-generation settings
    public boolean autoPregenOnJoin = false;
    public int autoPregenRadius = 32;
    public int autoPregenThreads = 2;

    private static LingConfig loadOrCreate() {
        if (LingCommon.isAvailable()) {
            var path = getConfigPath();
            if (Files.exists(path)) {
                try (FileReader reader = new FileReader(path.toFile())) {
                    var conf = GSON.fromJson(reader, LingConfig.class);
                    if (conf != null) {
                        conf.save();
                        return conf;
                    } else {
                        Logger.error("Failed to load voxy config, resetting");
                    }
                } catch (IOException e) {
                    Logger.error("Could not parse config", e);
                }
            }
            var config = new LingConfig();
            config.save();
            return config;
        } else {
            var config = new LingConfig();
            config.enabled = false;
            config.enableRendering = false;
            return config;
        }
    }

    public void save() {
        try {
            Files.writeString(getConfigPath(), GSON.toJson(this));
        } catch (IOException e) {
            Logger.error("Failed to write config file", e);
        }
        LingNeoForgeConfig.syncFromLingConfig();
    }

    private static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve("ling_horizons-config.json");
    }

    public boolean isRenderingEnabled() {
        return LingCommon.isAvailable() && this.enabled && this.enableRendering;
    }
}
