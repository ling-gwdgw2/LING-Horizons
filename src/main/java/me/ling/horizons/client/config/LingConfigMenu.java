package me.ling.horizons.client.config;

import me.ling.horizons.client.RenderStatistics;
import me.ling.horizons.client.config.SodiumConfigBuilder.*;
import me.ling.horizons.client.LingClient;
import me.ling.horizons.client.LingClientInstance;
import me.ling.horizons.client.core.IGetLingRenderSystem;
import me.ling.horizons.client.core.util.IrisUtil;
import me.ling.horizons.common.util.cpu.CpuLayout;
import me.ling.horizons.commonImpl.LingCommon;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@ConfigEntryPointForge("ling_horizons")
public class LingConfigMenu implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder B) {
        if (!LingCommon.isAvailable())
            return;// Dont even register the config if its not avalible

        var CFG = LingConfig.CONFIG;

        var cc = B.registerModOptions("ling_horizons", "ling_horizons", LingCommon.MOD_VERSION)
                .setIcon(ResourceLocation.parse("ling_horizons:icon.png"));

        final var RENDER_RELOAD = OptionFlag.REQUIRES_RENDERER_RELOAD.getId().toString();

        SodiumConfigBuilder.buildToSodium(B, cc, CFG::save, postOp -> {
            postOp.register("ling_horizons:update_threads", () -> {
                var instance = LingCommon.getInstance();
                if (instance != null) {
                    instance.updateDedicatedThreads();
                }
            }, "ling_horizons:enabled").register("ling_horizons:iris_reload", () -> IrisUtil.reload());
        },
                new Page(Component.translatable("voxy.config.general"),
                        new Group(
                                new BoolOption(
                                        "ling_horizons:enabled",
                                        Component.translatable("voxy.config.general.enabled"),
                                        () -> CFG.enabled, v -> {
                                            CFG.enabled = v;
                                            // we need to special case enabled, since the render reload flag runs befor
                                            // us and its quite important we get it right
                                            if (v && LingClientInstance.isInGame) {
                                                LingCommon.createInstance();
                                            }
                                        })
                                        .setPostChangeRunner(c -> {
                                            if (!c) {
                                                var vrsh = (IGetLingRenderSystem) Minecraft.getInstance().levelRenderer;
                                                if (vrsh != null) {
                                                    vrsh.shutdownRenderer();
                                                }
                                                LingCommon.shutdownInstance();
                                            }
                                        }).setPostChangeFlags(RENDER_RELOAD, "ling_horizons:iris_reload").setEnabler(null)),
                        new Group(
                                new IntOption(
                                        "ling_horizons:thread_count",
                                        Component.translatable("voxy.config.general.serviceThreads"),
                                        () -> CFG.serviceThreads, v -> CFG.serviceThreads = v,
                                        new Range(1, CpuLayout.getCoreCount(), 1))
                                        .setPostChangeFlags("ling_horizons:update_threads"),
                                new BoolOption(
                                        "ling_horizons:use_sodium_threads",
                                        Component.translatable("voxy.config.general.useSodiumBuilder"),
                                        () -> !CFG.dontUseSodiumBuilderThreads,
                                        v -> CFG.dontUseSodiumBuilderThreads = !v)
                                        .setPostChangeFlags("ling_horizons:update_threads")),
                        new Group(
                                new BoolOption(
                                        "ling_horizons:ingest_enabled",
                                        Component.translatable("voxy.config.general.ingest"),
                                        () -> CFG.ingestEnabled, v -> CFG.ingestEnabled = v)),
                        new Group(
                                new IntOption(
                                        "ling_horizons:vram_budget",
                                        Component.translatable("voxy.sodium.option.geometry_buffer_size"),
                                        () -> CFG.geometryBufferSizeMB, v -> CFG.geometryBufferSizeMB = v,
                                        new Range(0, 2048, 256))
                                        .setFormatter(v -> Component.literal(v == 0 ? "Auto" : (v + " MB")))
                                        .setPostChangeFlags(RENDER_RELOAD)),
                        new Group(
                                new BoolOption(
                                        "ling_horizons:auto_pregen_enabled",
                                        Component.translatable("voxy.sodium.option.auto_pregen"),
                                        () -> CFG.autoPregenOnJoin, v -> CFG.autoPregenOnJoin = v)
                                        .setPostChangeRunner(c -> {
                                            var mc = Minecraft.getInstance();
                                            if (c) {
                                                if (mc.player != null && mc.getSingleplayerServer() != null) {
                                                    me.ling.horizons.client.pregen.WorldPregenerator.getInstance()
                                                            .startPregen(CFG.autoPregenRadius);
                                                }
                                            } else {
                                                me.ling.horizons.client.pregen.WorldPregenerator.getInstance()
                                                        .cancelPregen();
                                            }
                                        }, "ling_horizons:enabled"),
                                new IntOption(
                                        "ling_horizons:auto_pregen_radius",
                                        Component.translatable("voxy.sodium.option.auto_pregen_radius"),
                                        () -> CFG.autoPregenRadius, v -> CFG.autoPregenRadius = v,
                                        new Range(16, 128, 16))
                                        .setFormatter(v -> Component.literal(v + " chunks"))
                                        .setPostChangeRunner(r -> {
                                            var pregen = me.ling.horizons.client.pregen.WorldPregenerator.getInstance();
                                            if (pregen.isRunning()) {
                                                pregen.startPregen(r);
                                            }
                                        }, "ling_horizons:auto_pregen_enabled"),
                                new IntOption(
                                        "ling_horizons:auto_pregen_threads",
                                        Component.translatable("voxy.sodium.option.auto_pregen_threads"),
                                        () -> CFG.autoPregenThreads, v -> CFG.autoPregenThreads = v,
                                        new Range(1, 4, 1))
                                        .setFormatter(v -> Component.literal(v + " Threads"))
                                        .setPostChangeRunner(t -> {
                                            var pregen = me.ling.horizons.client.pregen.WorldPregenerator.getInstance();
                                            if (pregen.isRunning()) {
                                                pregen.startPregen(CFG.autoPregenRadius);
                                            }
                                        }, "ling_horizons:auto_pregen_enabled")))
                        .setEnabler("ling_horizons:enabled"),
                new Page(Component.translatable("voxy.config.rendering"),
                        new Group(
                                new BoolOption(
                                        "ling_horizons:rendering",
                                        Component.translatable("voxy.config.general.rendering"),
                                        () -> CFG.enableRendering, v -> CFG.enableRendering = v)
                                        .setPostChangeRunner(c -> {
                                            var vrsh = (IGetLingRenderSystem) Minecraft.getInstance().levelRenderer;
                                            if (vrsh != null) {
                                                if (c) {
                                                    vrsh.createRenderer();
                                                } else {
                                                    vrsh.shutdownRenderer();
                                                }
                                            }
                                        }, "ling_horizons:enabled", RENDER_RELOAD)
                                        .setPostChangeFlags("ling_horizons:iris_reload")
                                        .setEnabler("ling_horizons:enabled")),
                        new Group(
                                new IntOption(
                                        "ling_horizons:subdivsize",
                                        Component.translatable("voxy.config.general.subDivisionSize"),
                                        () -> subDiv2ln(CFG.subDivisionSize), v -> CFG.subDivisionSize = ln2subDiv(v),
                                        new Range(0, SUBDIV_IN_MAX, 1))
                                        .setFormatter(
                                                v -> Component.literal(Integer.toString(Math.round(ln2subDiv(v)))))
                                        .setPostChangeFlags(RENDER_RELOAD),
                                new IntOption(
                                        "ling_horizons:render_distance",
                                        Component.translatable("voxy.config.general.renderDistance"),
                                        () -> CFG.sectionRenderDistance, v -> CFG.sectionRenderDistance = v,
                                        new Range(2, 64, 1))
                                        .setFormatter(v -> Component.literal(Integer.toString(v * 32)))// Top level rd
                                                                                                       // == 32 chunks
                                        .setPostChangeRunner(c -> {
                                            var vrsh = (IGetLingRenderSystem) Minecraft.getInstance().levelRenderer;
                                            if (vrsh != null) {
                                                var vrs = vrsh.getLingRenderSystem();
                                                if (vrs != null) {
                                                    vrs.setRenderDistance(c);
                                                }
                                            }
                                        }, "ling_horizons:rendering", RENDER_RELOAD)),
                        new Group(
                                new IntOption(
                                        "ling_horizons:lod_boundary_buffer",
                                        Component.translatable("voxy.sodium.option.lod_boundary_buffer"),
                                        () -> CFG.lodBoundaryBuffer, v -> CFG.lodBoundaryBuffer = v,
                                        new Range(0, 4, 1))
                                        .setPostChangeFlags(RENDER_RELOAD),
                                new IntOption(
                                        "ling_horizons:earth_curve_ratio",
                                        Component.translatable("voxy.sodium.option.earth_curve_ratio"),
                                        () -> CFG.earthCurveRatio, v -> CFG.earthCurveRatio = v,
                                        new Range(0, 500, 10))
                                        .setFormatter(v -> Component
                                                .literal(v == 0 ? "Off" : (v < 50 ? "50" : Integer.toString(v))))
                                        .setPostChangeFlags(RENDER_RELOAD)),
                        new Group(
                                new BoolOption(
                                        "ling_horizons:eviromental_fog",
                                        Component.translatable("voxy.config.general.environmental_fog"),
                                        () -> CFG.useEnvironmentalFog, v -> CFG.useEnvironmentalFog = v)
                                        .setPostChangeFlags(RENDER_RELOAD),
                                new BoolOption(
                                        "ling_horizons:water_ssr_reflection",
                                        Component.translatable("voxy.sodium.option.water_ssr_reflection"),
                                        () -> CFG.enableWaterSSR, v -> CFG.enableWaterSSR = v)
                                        .setPostChangeFlags(RENDER_RELOAD, "ling_horizons:iris_reload"),
                                new BoolOption(
                                        "ling_horizons:distant_shader_shadows",
                                        Component.translatable("voxy.sodium.option.distant_shader_shadows"),
                                        () -> CFG.enableDistantShaderShadows, v -> CFG.enableDistantShaderShadows = v)
                                        .setPostChangeFlags(RENDER_RELOAD, "ling_horizons:iris_reload")),
                        new Group(
                                new BoolOption(
                                        "ling_horizons:render_debug",
                                        Component.translatable("voxy.config.general.render_statistics"),
                                        () -> RenderStatistics.enabled, v -> RenderStatistics.enabled = v)
                                        .setPostChangeFlags(RENDER_RELOAD)))
                        .setEnablerAND("ling_horizons:enabled", "ling_horizons:rendering"));

    }

    private static final int SUBDIV_IN_MAX = 100;
    private static final double SUBDIV_MIN = 28;
    private static final double SUBDIV_MAX = 256;
    private static final double SUBDIV_CONST = Math.log(SUBDIV_MAX / SUBDIV_MIN) / Math.log(2);

    // In range is 0->200
    // Out range is 28->256
    private static float ln2subDiv(int in) {
        return (float) (SUBDIV_MIN * Math.pow(2, SUBDIV_CONST * ((double) in / SUBDIV_IN_MAX)));
    }

    // In range is ... any?
    // Out range is 0->200
    private static int subDiv2ln(float in) {
        return (int) (((Math.log(((double) in) / SUBDIV_MIN) / Math.log(2)) / SUBDIV_CONST) * SUBDIV_IN_MAX);
    }
}
