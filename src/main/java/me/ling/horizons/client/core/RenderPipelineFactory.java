package me.ling.horizons.client.core;

import me.ling.horizons.client.core.rendering.hierachical.AsyncNodeManager;
import me.ling.horizons.client.core.rendering.hierachical.HierarchicalOcclusionTraverser;
import me.ling.horizons.client.core.rendering.hierachical.NodeCleaner;
import me.ling.horizons.client.core.util.IrisUtil;
import me.ling.horizons.client.iris.IGetIrisLingPipelineData;
import me.ling.horizons.common.Logger;
import net.irisshaders.iris.Iris;

import java.util.function.BooleanSupplier;

public class RenderPipelineFactory {
    public static AbstractRenderPipeline createPipeline(AsyncNodeManager nodeManager, NodeCleaner nodeCleaner, HierarchicalOcclusionTraverser traversal, BooleanSupplier frexSupplier) {
        AbstractRenderPipeline pipeline = null;
        if (IrisUtil.IRIS_INSTALLED && IrisUtil.SHADER_SUPPORT) {
            pipeline = createIrisPipeline(nodeManager, nodeCleaner, traversal, frexSupplier);
        }
        if (pipeline == null) {
            pipeline = new NormalRenderPipeline(nodeManager, nodeCleaner, traversal, frexSupplier);
        }
        return pipeline;
    }

    private static AbstractRenderPipeline createIrisPipeline(AsyncNodeManager nodeManager, NodeCleaner nodeCleaner, HierarchicalOcclusionTraverser traversal, BooleanSupplier frexSupplier) {
        var irisPipe = Iris.getPipelineManager().getPipelineNullable();
        if (irisPipe == null) {
            return null;
        }
        if (irisPipe instanceof IGetIrisLingPipelineData getVoxyPipeData) {
            var pipeData = getVoxyPipeData.voxy$getPipelineData();
            if (pipeData == null) {
                return null;
            }
            Logger.info("Creating voxy iris render pipeline");
            try {
                return new IrisLingRenderPipeline(pipeData, nodeManager, nodeCleaner, traversal, frexSupplier);
            } catch (Exception e) {
                Logger.error("Failed to create iris render pipeline", e);
                IrisUtil.disableIrisShaders();
                return null;
            }
        }
        return null;
    }
}
