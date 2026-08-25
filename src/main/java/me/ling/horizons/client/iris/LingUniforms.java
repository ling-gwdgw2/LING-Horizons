package me.ling.horizons.client.iris;

import me.ling.horizons.client.config.LingConfig;
import me.ling.horizons.client.core.IGetLingRenderSystem;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.function.Supplier;

import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;

public class LingUniforms {

    public static Matrix4f getViewProjection() {//This is 1 frame late ;-; cries, since the update occurs _before_ the voxy render pipeline
        var getVrs = (IGetLingRenderSystem) Minecraft.getInstance().levelRenderer;
        if (getVrs == null || getVrs.getLingRenderSystem() == null) {
            return new Matrix4f();
        }
        var vrs = getVrs.getLingRenderSystem();
        return new Matrix4f(vrs.getViewport().MVP);
    }

    public static Matrix4f getModelView() {//This is 1 frame late ;-; cries, since the update occurs _before_ the voxy render pipeline
        var getVrs = (IGetLingRenderSystem) Minecraft.getInstance().levelRenderer;
        if (getVrs == null || getVrs.getLingRenderSystem() == null) {
            return new Matrix4f();
        }
        var vrs = getVrs.getLingRenderSystem();
        return new Matrix4f(vrs.getViewport().modelView);
    }

    public static Matrix4f getProjection() {//This is 1 frame late ;-; cries, since the update occurs _before_ the voxy render pipeline
        var getVrs = (IGetLingRenderSystem) Minecraft.getInstance().levelRenderer;
        if (getVrs == null || getVrs.getLingRenderSystem() == null) {
            return new Matrix4f();
        }
        var vrs = getVrs.getLingRenderSystem();
        var mat = vrs.getViewport().projection;
        if (mat == null) {
            return new Matrix4f();
        }
        return new Matrix4f(mat);
    }

    public static void addUniforms(UniformHolder uniforms) {
        uniforms
                .uniform1i(PER_FRAME, "vxRenderDistance", () -> LingConfig.CONFIG.sectionRenderDistance * 32)
                .uniformMatrix(PER_FRAME, "vxViewProj", LingUniforms::getViewProjection)
                .uniformMatrix(PER_FRAME, "vxViewProjInv", new Inverted(LingUniforms::getViewProjection))
                .uniformMatrix(PER_FRAME, "vxViewProjPrev", new PreviousMat(LingUniforms::getViewProjection))
                .uniformMatrix(PER_FRAME, "vxModelView", LingUniforms::getModelView)
                .uniformMatrix(PER_FRAME, "vxModelViewInv", new Inverted(LingUniforms::getModelView))
                .uniformMatrix(PER_FRAME, "vxModelViewPrev", new PreviousMat(LingUniforms::getModelView))
                .uniformMatrix(PER_FRAME, "vxProj", LingUniforms::getProjection)
                .uniformMatrix(PER_FRAME, "vxProjInv", new Inverted(LingUniforms::getProjection))
                .uniformMatrix(PER_FRAME, "vxProjPrev", new PreviousMat(LingUniforms::getProjection))

                // Distant Horizons emulation uniforms (for atmospheric fog, sky blending & shaderpacks)
                .uniform1f(PER_FRAME, "dhNearPlane", () -> {
                    int rdChunks = Minecraft.getInstance().options.getEffectiveRenderDistance();
                    float vanillaDistanceBlocks = rdChunks * 16.0f;
                    return me.ling.horizons.client.LingClient.disableSodiumChunkRender() ? 0.1f : vanillaDistanceBlocks;
                })
                .uniform1f(PER_FRAME, "dhFarPlane", () -> (float) (LingConfig.CONFIG.sectionRenderDistance * 32 * 16))
                .uniform1i(PER_FRAME, "dhRenderDistance", () -> LingConfig.CONFIG.sectionRenderDistance * 32)
                .uniform1i(PER_FRAME, "dhMinRenderDistance", () -> Minecraft.getInstance().options.getEffectiveRenderDistance())
                .uniform1i(PER_FRAME, "dhMaxRenderDistance", () -> LingConfig.CONFIG.sectionRenderDistance * 32)
                .uniform1f(PER_FRAME, "dhMinFogDistance", () -> (float) (LingConfig.CONFIG.sectionRenderDistance * 32 * 16 * 0.75f))
                .uniform1f(PER_FRAME, "dhMaxFogDistance", () -> (float) (LingConfig.CONFIG.sectionRenderDistance * 32 * 16))
                .uniformMatrix(PER_FRAME, "dhProjection", LingUniforms::getProjection)
                .uniformMatrix(PER_FRAME, "dhProjectionInverse", new Inverted(LingUniforms::getProjection))
                .uniformMatrix(PER_FRAME, "dhPreviousProjection", new PreviousMat(LingUniforms::getProjection))
                .uniformMatrix(PER_FRAME, "dhModelView", LingUniforms::getModelView)
                .uniformMatrix(PER_FRAME, "dhModelViewInverse", new Inverted(LingUniforms::getModelView))
                .uniformMatrix(PER_FRAME, "dhPreviousModelView", new PreviousMat(LingUniforms::getModelView))
                .uniformMatrix(PER_FRAME, "dhViewProj", LingUniforms::getViewProjection)
                .uniformMatrix(PER_FRAME, "dhViewProjInverse", new Inverted(LingUniforms::getViewProjection))
                .uniformMatrix(PER_FRAME, "dhPreviousViewProj", new PreviousMat(LingUniforms::getViewProjection));
    }




    private record Inverted(Supplier<Matrix4fc> parent) implements Supplier<Matrix4fc> {
        private Inverted(Supplier<Matrix4fc> parent) {
            this.parent = parent;
        }

        public Matrix4fc get() {
            Matrix4f copy = new Matrix4f(this.parent.get());
            copy.invert();
            return copy;
        }

        public Supplier<Matrix4fc> parent() {
            return this.parent;
        }
    }

    private static class PreviousMat implements Supplier<Matrix4fc> {
        private final Supplier<Matrix4fc> parent;
        private Matrix4f previous;

        PreviousMat(Supplier<Matrix4fc> parent) {
            this.parent = parent;
            this.previous = new Matrix4f();
        }

        public Matrix4fc get() {
            Matrix4f previous = this.previous;
            this.previous = new Matrix4f(this.parent.get());
            return previous;
        }
    }
}
