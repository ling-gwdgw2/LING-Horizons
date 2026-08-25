package me.ling.horizons.client.iris;

import me.ling.horizons.client.core.IGetLingRenderSystem;
import me.ling.horizons.client.core.IrisLingRenderPipeline;
import net.irisshaders.iris.gl.sampler.GlSampler;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.minecraft.client.Minecraft;

public class LingSamplers {
    public static void addSamplers(IrisRenderingPipeline pipeline, SamplerHolder samplers) {
        String[] opaqueNames = new String[]{"vxDepthTexOpaque", "dhDepthTex1", "dhTerrainDepthTex", "dhDepthTexOpaque"};
        String[] translucentNames = new String[]{"vxDepthTexTrans", "dhDepthTex", "dhDepthTex0", "dhWaterDepthTex"};

        samplers.addDynamicSampler(TextureType.TEXTURE_2D, () -> {
            var pipeData = ((IGetIrisLingPipelineData)pipeline).voxy$getPipelineData();
            if (pipeData != null && pipeData.thePipeline != null) {
                var dt = pipeData.thePipeline.fb.getDepthTex();
                if (dt != null) {
                    return dt.id;
                }
            }
            if (Minecraft.getInstance().levelRenderer instanceof IGetLingRenderSystem getVrs) {
                var vrs = getVrs.getLingRenderSystem();
                if (vrs != null && vrs.getPipeline() != null && vrs.getPipeline().fb != null) {
                    var dt = vrs.getPipeline().fb.getDepthTex();
                    if (dt != null) {
                        return dt.id;
                    }
                }
            }
            return 0;
        }, GlSampler.NEAREST, opaqueNames);

        samplers.addDynamicSampler(TextureType.TEXTURE_2D, () -> {
            var pipeData = ((IGetIrisLingPipelineData)pipeline).voxy$getPipelineData();
            if (pipeData != null && pipeData.thePipeline != null) {
                var dt = pipeData.thePipeline.fbTranslucent.getDepthTex();
                if (dt != null) {
                    return dt.id;
                }
            }
            if (Minecraft.getInstance().levelRenderer instanceof IGetLingRenderSystem getVrs) {
                var vrs = getVrs.getLingRenderSystem();
                if (vrs != null && vrs.getPipeline() instanceof IrisLingRenderPipeline irp && irp.fbTranslucent != null) {
                    var dt = irp.fbTranslucent.getDepthTex();
                    if (dt != null) {
                        return dt.id;
                    }
                }
                if (vrs != null && vrs.getPipeline() != null && vrs.getPipeline().fb != null) {
                    var dt = vrs.getPipeline().fb.getDepthTex();
                    if (dt != null) {
                        return dt.id;
                    }
                }
            }
            return 0;
        }, GlSampler.NEAREST, translucentNames);
    }
}
