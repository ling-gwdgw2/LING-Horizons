package me.ling.horizons.client.config;

import me.ling.horizons.client.RenderStatistics;
import me.ling.horizons.common.util.cpu.CpuLayout;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge config integration for Voxy.
 * Provides a built-in config screen accessible from the Mods menu.
 *
 * This wraps the existing LingConfig and syncs values between the two systems.
 */
@EventBusSubscriber(modid = "ling_horizons", bus = EventBusSubscriber.Bus.MOD)
public class LingNeoForgeConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // General settings
    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable Voxy LOD rendering system")
            .define("enabled", true);

    private static final ModConfigSpec.BooleanValue ENABLE_RENDERING = BUILDER
            .comment("Enable LOD terrain rendering (can be disabled while keeping data ingestion)")
            .define("enableRendering", true);

    private static final ModConfigSpec.BooleanValue INGEST_ENABLED = BUILDER
            .comment("Enable automatic chunk data ingestion for LOD generation")
            .define("ingestEnabled", true);

    // Performance settings
    private static final ModConfigSpec.IntValue SECTION_RENDER_DISTANCE = BUILDER
            .comment("LOD section render distance (multiplied by 32 for actual chunk distance)",
                    "Example: 16 = 512 chunks render distance")
            .defineInRange("sectionRenderDistance", 16, 2, 64);

    private static final ModConfigSpec.IntValue SERVICE_THREADS = BUILDER
            .comment("Number of background threads for LOD processing",
                    "Default is based on CPU core count.")
            .defineInRange("serviceThreads", Math.max((int) (CpuLayout.getCoreCount() / 1.5), 1), 1,
                    CpuLayout.getCoreCount());

    private static final ModConfigSpec.DoubleValue SUB_DIVISION_SIZE = BUILDER
            .comment("Subdivision size for LOD rendering (28-256)",
                    "Lower = more detailed LODs but more GPU load")
            .defineInRange("subDivisionSize", 64.0, 28.0, 256.0);

    // Visual settings
    private static final ModConfigSpec.BooleanValue USE_ENVIRONMENTAL_FOG = BUILDER
            .comment("Apply environmental fog to LOD terrain")
            .define("useEnvironmentalFog", true);

    private static final ModConfigSpec.BooleanValue ENABLE_WATER_SSR = BUILDER
            .comment("Enable Screen-Space Reflections (SSR) and wave normal enhancement for LOD water")
            .define("enableWaterSSR", true);

    private static final ModConfigSpec.BooleanValue ENABLE_DISTANT_SHADER_SHADOWS = BUILDER
            .comment("Enable distant shader shadows, cloud shadows, and lighting on LODs via Iris",
                    "Disable to save GPU performance on low-end systems.")
            .define("enableDistantShaderShadows", true);

    // Advanced settings
    private static final ModConfigSpec.BooleanValue DONT_USE_SODIUM_BUILDER_THREADS = BUILDER
            .comment("Don't share threads with Sodium's chunk builder")
            .define("dontUseSodiumBuilderThreads", false);

    // LOD boundary buffer (overdraw/overlap)
    private static final ModConfigSpec.IntValue LOD_BOUNDARY_BUFFER = BUILDER
            .comment("LOD boundary overlap in blocks (like DH's overdraw prevention)",
                    "Controls how much LODs overlap with vanilla chunk edges.",
                    "Higher values = more overlap = smoother transitions but slight overdraw.",
                    "0 = exact match (may have gaps), 1 = minimal overlap, 2-4 = smoother for fast flight")
            .defineInRange("lodBoundaryBuffer", 1, 0, 4);

    // World curvature (experimental)
    private static final ModConfigSpec.IntValue EARTH_CURVE_RATIO = BUILDER
            .comment("World curvature effect - simulates standing on a spherical planet",
                    "0 = disabled (flat world)",
                    "1 = real Earth curvature (6371km radius)",
                    "Higher values = more extreme curvature (smaller planet effect)",
                    "Valid range: 0 (off), or 50-5000. Values 1-49 are auto-corrected to 50.",
                    "Inspired by Distant Horizons' earth curvature feature")
            .defineInRange("earthCurveRatio", 0, 0, 5000);

    // VRAM Geometry Budget
    private static final ModConfigSpec.IntValue GEOMETRY_BUFFER_SIZE_MB = BUILDER
            .comment("VRAM Geometry Budget in MB (0 = Auto detection based on GPU VRAM, e.g. 512, 768, 1024, 1536)",
                    "Prevents VRAM overflow and crashes on GPUs with 4GB/6GB/8GB VRAM.")
            .defineInRange("geometryBufferSizeMB", 0, 0, 2048);

    // World Pre-generation settings
    private static final ModConfigSpec.BooleanValue AUTO_PREGEN_ON_JOIN = BUILDER
            .comment("Automatically pre-generate LOD chunks in radius upon joining a Singleplayer world")
            .define("autoPregenOnJoin", false);

    private static final ModConfigSpec.IntValue AUTO_PREGEN_RADIUS = BUILDER
            .comment("Radius of chunks to auto pre-generate around world spawn (16-128 chunks)")
            .defineInRange("autoPregenRadius", 32, 16, 128);

    private static final ModConfigSpec.IntValue AUTO_PREGEN_THREADS = BUILDER
            .comment("Number of background worker threads dedicated to World Pre-generation")
            .defineInRange("autoPregenThreads", 2, 1, 4);

    // Debug settings
    private static final ModConfigSpec.BooleanValue RENDER_STATISTICS = BUILDER
            .comment("Show render statistics in F3 debug screen",
                    "Displays LOD traversal counts, visible sections, and quad counts")
            .define("renderStatistics", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Register the config with NeoForge.
     * Call this during mod construction.
     */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "ling_horizons-client.toml");
    }

    /**
     * Sync NeoForge config values to LingConfig.
     */
    private static void syncToLingConfig() {
        LingConfig.CONFIG.enabled = ENABLED.get();
        LingConfig.CONFIG.enableRendering = ENABLE_RENDERING.get();
        LingConfig.CONFIG.ingestEnabled = INGEST_ENABLED.get();
        LingConfig.CONFIG.sectionRenderDistance = SECTION_RENDER_DISTANCE.get();
        LingConfig.CONFIG.serviceThreads = SERVICE_THREADS.get();
        LingConfig.CONFIG.subDivisionSize = SUB_DIVISION_SIZE.get().floatValue();
        LingConfig.CONFIG.useEnvironmentalFog = USE_ENVIRONMENTAL_FOG.get();
        LingConfig.CONFIG.dontUseSodiumBuilderThreads = DONT_USE_SODIUM_BUILDER_THREADS.get();
        LingConfig.CONFIG.lodBoundaryBuffer = LOD_BOUNDARY_BUFFER.get();
        LingConfig.CONFIG.earthCurveRatio = EARTH_CURVE_RATIO.get();
        LingConfig.CONFIG.geometryBufferSizeMB = GEOMETRY_BUFFER_SIZE_MB.get();
        LingConfig.CONFIG.enableWaterSSR = ENABLE_WATER_SSR.get();
        LingConfig.CONFIG.enableDistantShaderShadows = ENABLE_DISTANT_SHADER_SHADOWS.get();
        LingConfig.CONFIG.autoPregenOnJoin = AUTO_PREGEN_ON_JOIN.get();
        LingConfig.CONFIG.autoPregenRadius = AUTO_PREGEN_RADIUS.get();
        LingConfig.CONFIG.autoPregenThreads = AUTO_PREGEN_THREADS.get();

        // RenderStatistics is a runtime-only setting (not saved to JSON)
        RenderStatistics.enabled = RENDER_STATISTICS.get();
    }

    /**
     * Sync LingConfig values back to NeoForge TOML spec and save to disk.
     */
    public static void syncFromLingConfig() {
        try {
            if (SPEC.isLoaded()) {
                ENABLED.set(LingConfig.CONFIG.enabled);
                ENABLE_RENDERING.set(LingConfig.CONFIG.enableRendering);
                INGEST_ENABLED.set(LingConfig.CONFIG.ingestEnabled);
                SECTION_RENDER_DISTANCE.set(LingConfig.CONFIG.sectionRenderDistance);
                SERVICE_THREADS.set(LingConfig.CONFIG.serviceThreads);
                SUB_DIVISION_SIZE.set((double) LingConfig.CONFIG.subDivisionSize);
                USE_ENVIRONMENTAL_FOG.set(LingConfig.CONFIG.useEnvironmentalFog);
                DONT_USE_SODIUM_BUILDER_THREADS.set(LingConfig.CONFIG.dontUseSodiumBuilderThreads);
                LOD_BOUNDARY_BUFFER.set(LingConfig.CONFIG.lodBoundaryBuffer);
                EARTH_CURVE_RATIO.set(LingConfig.CONFIG.earthCurveRatio);
                GEOMETRY_BUFFER_SIZE_MB.set(LingConfig.CONFIG.geometryBufferSizeMB);
                ENABLE_WATER_SSR.set(LingConfig.CONFIG.enableWaterSSR);
                ENABLE_DISTANT_SHADER_SHADOWS.set(LingConfig.CONFIG.enableDistantShaderShadows);
                AUTO_PREGEN_ON_JOIN.set(LingConfig.CONFIG.autoPregenOnJoin);
                AUTO_PREGEN_RADIUS.set(LingConfig.CONFIG.autoPregenRadius);
                AUTO_PREGEN_THREADS.set(LingConfig.CONFIG.autoPregenThreads);
                RENDER_STATISTICS.set(RenderStatistics.enabled);
                SPEC.save();
            }
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            syncFromLingConfig();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            syncToLingConfig();
        }
    }

    // Getters for direct access (optional, can use LingConfig.CONFIG instead)
    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static boolean isRenderingEnabled() {
        return ENABLE_RENDERING.get();
    }

    public static boolean isIngestEnabled() {
        return INGEST_ENABLED.get();
    }

    public static int getSectionRenderDistance() {
        return SECTION_RENDER_DISTANCE.get();
    }

    public static int getServiceThreads() {
        return SERVICE_THREADS.get();
    }

    public static float getSubDivisionSize() {
        return SUB_DIVISION_SIZE.get().floatValue();
    }

    public static boolean useEnvironmentalFog() {
        return USE_ENVIRONMENTAL_FOG.get();
    }

    public static boolean dontUseSodiumBuilderThreads() {
        return DONT_USE_SODIUM_BUILDER_THREADS.get();
    }

    public static int getLodBoundaryBuffer() {
        return LOD_BOUNDARY_BUFFER.get();
    }

    public static boolean isRenderStatisticsEnabled() {
        return RENDER_STATISTICS.get();
    }

    public static int getEarthCurveRatio() {
        return EARTH_CURVE_RATIO.get();
    }

    public static boolean isDistantShaderShadowsEnabled() {
        return ENABLE_DISTANT_SHADER_SHADOWS.get();
    }
}
