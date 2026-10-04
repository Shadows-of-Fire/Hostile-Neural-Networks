package dev.shadowsoffire.hostilenetworks.client.ghost;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SubmitNodeCollector} that turns everything submitted through it into a translucent "ghost".
 * <p>
 * This is the 26.1 replacement for the 1.21 {@code WrappedRTBuffer} + {@code GhostVertexBuilder} + {@code MixinRenderType}
 * trio. Blend state now lives on immutable {@code RenderPipeline}s, so instead of forcing blending at draw time, every
 * submission is rewritten onto a translucent {@link RenderType} bound to the same texture (see {@link #ghostType}), and
 * the submission's color has its alpha scaled by {@link #alpha}.
 * <ul>
 * <li>Model, model part and block model submissions are forwarded with a translucent render type and ghost-tinted color.</li>
 * <li>Custom geometry is forwarded with a translucent render type and a {@link GhostVertexConsumer}.</li>
 * <li>Item submissions (e.g. the block inside an {@code ItemEntity} for block data models) are re-emitted as custom
 * geometry, since vanilla would otherwise pick each quad's opaque item render type at draw time.</li>
 * <li>Decorations that make no sense on a ghost (shadows, name tags, text, flames, leashes, particles, moving / breaking
 * blocks) are dropped.</li>
 * </ul>
 * {@link #order(int)} returns another ghost collector, so render layers that submit through an ordered collector
 * (armor, eyes, saddles, ...) are ghosted as well.
 * <p>
 * Reading the texture of an arbitrary {@link RenderType} requires the access transformer entries for
 * {@code RenderType#state}, {@code RenderSetup#textures} and {@code RenderSetup$TextureBinding}.
 */
public final class GhostCollector implements SubmitNodeCollector {

    /** Memoized translucent render types, keyed by the original (opaque) render type. Render thread only. */
    private static final Map<RenderType, RenderType> GHOST_TYPES = new IdentityHashMap<>();

    private final SubmitNodeCollector root;
    private final OrderedSubmitNodeCollector inner;
    private final int alpha;

    /**
     * @param root  The collector submissions are forwarded to.
     * @param alpha The ghost alpha (0-255). Existing alpha is multiplied by this value, not replaced.
     */
    public GhostCollector(SubmitNodeCollector root, int alpha) {
        this(root, root, alpha);
    }

    private GhostCollector(SubmitNodeCollector root, OrderedSubmitNodeCollector inner, int alpha) {
        this.root = root;
        this.inner = inner;
        this.alpha = alpha & 0xFF;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new GhostCollector(this.root, this.root.order(order), this.alpha);
    }

    // --- forwarded with translucent render type + ghost color ---

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor,
        @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        this.inner.submitModel(model, state, poseStack, ghostType(renderType), lightCoords, overlayCoords, this.ghostColor(tintedColor), sprite, 0, null);
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite,
        boolean sheeted, boolean hasFoil, int tintedColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
        this.inner.submitModelPart(modelPart, poseStack, ghostType(renderType), lightCoords, overlayCoords, sprite, sheeted, false, this.ghostColor(tintedColor), null, 0);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        int alpha = this.alpha;
        this.inner.submitCustomGeometry(poseStack, ghostType(renderType), (pose, buffer) -> customGeometryRenderer.render(pose, new GhostVertexConsumer(buffer, alpha)));
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        this.inner.submitBlockModel(poseStack, ghostType(renderType), parts, this.ghostTints(tintLayers), lightCoords, overlayCoords, 0);
    }

    @Override
    public void submitMultiLayerBlockModel(PoseStack poseStack, List<BlockStateModelPart> modelParts, boolean translucent, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        this.inner.submitMultiLayerBlockModel(poseStack, modelParts, translucent, this.ghostTints(tintLayers), lightCoords, overlayCoords, 0);
    }

    /**
     * Item quads carry their own (opaque) render type that is only resolved inside {@code ItemFeatureRenderer}, so the
     * submission is converted into custom geometry on the matching translucent item sheet. Mirrors
     * {@code ItemFeatureRenderer#renderItem} minus foil support.
     */
    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads,
        ItemStackRenderState.FoilType foilType) {
        // The quad list belongs to a reusable render state, so copy it before deferring.
        Map<RenderType, List<BakedQuad>> groups = new LinkedHashMap<>();
        for (BakedQuad quad : quads) {
            groups.computeIfAbsent(ghostItemType(quad), k -> new ArrayList<>()).add(quad);
        }

        for (Map.Entry<RenderType, List<BakedQuad>> entry : groups.entrySet()) {
            List<BakedQuad> group = entry.getValue();
            this.inner.submitCustomGeometry(poseStack, entry.getKey(), (pose, buffer) -> {
                QuadInstance instance = new QuadInstance();
                instance.setLightCoords(lightCoords);
                instance.setOverlayCoords(overlayCoords);
                for (BakedQuad quad : group) {
                    BakedQuad.MaterialInfo material = quad.materialInfo();
                    int tint = material.isTinted() && material.tintIndex() >= 0 && material.tintIndex() < tintLayers.length ? tintLayers[material.tintIndex()] : -1;
                    instance.setColor(this.ghostColor(tint));
                    buffer.putBakedQuad(pose, quad, instance);
                }
            });
        }
    }

    // --- decorations that do not belong on a ghost ---

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {}

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, double distanceToCameraSq,
        CameraRenderState camera) {}

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color,
        int backgroundColor, int outlineColor) {}

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {}

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {}

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {}

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {}

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer particleGroupRenderer) {}

    // --- helpers ---

    /**
     * Multiplies the alpha of an ARGB color by the ghost alpha, preserving the RGB tint and any existing translucency.
     */
    private int ghostColor(int color) {
        return ARGB.multiply(color, ARGB.color(this.alpha, 0xFFFFFF));
    }

    private int @Nullable [] ghostTints(int @Nullable [] tintLayers) {
        if (tintLayers == null) return null;
        int[] copy = new int[tintLayers.length];
        for (int i = 0; i < tintLayers.length; i++) {
            copy[i] = this.ghostColor(tintLayers[i]);
        }
        return copy;
    }

    /**
     * Resolves the translucent render type to draw a ghost of geometry that would normally use {@code original}.
     * <p>
     * Render types that already blend are returned unchanged, since their vertex alpha is honored. Otherwise the first
     * texture bound to the type is looked up and {@link RenderTypes#entityTranslucentCullItemTarget} is used (translucent
     * blending, backface culling, lightmap and overlay support). Types with no texture are returned unchanged, which
     * renders them opaque rather than failing.
     */
    public static RenderType ghostType(RenderType original) {
        if (original.hasBlending()) return original;
        return GHOST_TYPES.computeIfAbsent(original, GhostCollector::createGhostType);
    }

    private static RenderType createGhostType(RenderType original) {
        for (RenderSetup.TextureBinding binding : original.state.textures.values()) {
            Identifier texture = binding.location();
            if (texture != null) {
                return RenderTypes.entityTranslucentCullItemTarget(texture);
            }
        }
        return original;
    }

    private static RenderType ghostItemType(BakedQuad quad) {
        RenderType original = quad.materialInfo().itemRenderType();
        if (original.hasBlending()) return original;
        boolean blockAtlas = TextureAtlas.LOCATION_BLOCKS.equals(quad.materialInfo().sprite().atlasLocation());
        return blockAtlas ? Sheets.translucentBlockItemSheet() : Sheets.translucentItemSheet();
    }

}
