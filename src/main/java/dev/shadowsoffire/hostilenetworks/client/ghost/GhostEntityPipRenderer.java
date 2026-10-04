package dev.shadowsoffire.hostilenetworks.client.ghost;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Picture-in-picture renderer that draws a {@link GhostEntityRenderState} as a translucent ghost.
 * <p>
 * Mirrors the vanilla {@code GuiEntityRenderer}, but routes the submission through a {@link GhostCollector}. The picture
 * texture is cleared to transparent and blitted with premultiplied alpha, so the ghost's translucency survives
 * compositing onto the screen.
 */
public class GhostEntityPipRenderer extends PictureInPictureRenderer<GhostEntityRenderState> {

    private final EntityRenderDispatcher entityRenderDispatcher;

    public GhostEntityPipRenderer(MultiBufferSource.BufferSource bufferSource, EntityRenderDispatcher entityRenderDispatcher) {
        super(bufferSource);
        this.entityRenderDispatcher = entityRenderDispatcher;
    }

    @Override
    public Class<GhostEntityRenderState> getRenderStateClass() {
        return GhostEntityRenderState.class;
    }

    @Override
    protected void renderToTexture(GhostEntityRenderState state, PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);

        Vector3f translation = state.translation();
        poseStack.translate(translation.x, translation.y, translation.z);
        poseStack.mulPose(state.rotation());

        CameraRenderState camera = new CameraRenderState();
        Quaternionf overriddenCameraAngle = state.overrideCameraAngle();
        if (overriddenCameraAngle != null) {
            camera.orientation = overriddenCameraAngle.conjugate(new Quaternionf()).rotateY((float) Math.PI);
        }

        FeatureRenderDispatcher features = mc.gameRenderer.getFeatureRenderDispatcher();
        Vector3f offset = state.entityOffset();
        this.entityRenderDispatcher.submit(state.renderState(), camera, offset.x, offset.y, offset.z, poseStack, new GhostCollector(features.getSubmitNodeStorage(), state.alpha()));
        features.renderAllFeatures();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "hnn_ghost_entity";
    }

}
