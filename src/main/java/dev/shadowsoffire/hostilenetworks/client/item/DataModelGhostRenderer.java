package dev.shadowsoffire.hostilenetworks.client.item;

import java.util.function.Consumer;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.shadowsoffire.hostilenetworks.client.ghost.GhostCollector;
import dev.shadowsoffire.hostilenetworks.client.ghost.GhostEntityStates;
import dev.shadowsoffire.hostilenetworks.client.item.DataModelItemModel.Argument;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Draws the spinning, translucent ghost of a Data Model's display entity inside the item.
 * <p>
 * The per-context transforms are those of the 1.21 {@code DataModelItemStackRenderer#renderEntityInInventory}; the pose
 * handed to {@link #submit} already includes the base model's display transform, which ends in the same
 * {@code translate(-0.5, -0.5, -0.5)} the old item renderer applied, so they port unchanged.
 */
public final class DataModelGhostRenderer implements SpecialModelRenderer<Argument> {

    public static final DataModelGhostRenderer INSTANCE = new DataModelGhostRenderer();

    private static final int GHOST_ALPHA = 0xBB;
    /** Only name tags and flames read the camera, and the ghost collector drops both, so a default state suffices. */
    private static final CameraRenderState CAMERA = new CameraRenderState();

    private DataModelGhostRenderer() {}

    @Override
    public void submit(@Nullable Argument arg, PoseStack pose, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (arg == null || !arg.model().isBound()) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        DisplayEntity display = arg.model().get().displayEntity(level);
        Entity entity = ClientEntityCache.computeIfAbsent(display, level);
        if (entity == null) return;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        EntityRenderState state = GhostEntityStates.extract(entity, partialTick);
        if (state == null) return;

        ItemDisplayContext ctx = arg.context();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        float scale = display.scale();
        if (ctx == ItemDisplayContext.FIXED) {
            scale *= 0.4F;
            pose.scale(scale, scale, scale);
            pose.mulPose(Axis.XN.rotationDegrees(90));
            pose.mulPose(Axis.YN.rotationDegrees(180));
        }
        else if (ctx == ItemDisplayContext.GUI) {
            scale *= 0.4F;
            pose.scale(scale, scale, scale);
            pose.translate(0, -0.32F / scale, 0);
        }
        else {
            scale *= 0.25F;
            pose.scale(scale, scale, scale);
            double bob = 0.12 + 0.05 * Math.sin((entity.tickCount + partialTick) / 12);
            pose.translate(0, (bob * 0.25) / scale, 0);
        }

        float rotation = -30;
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) rotation = 30;
        if (ctx == ItemDisplayContext.FIXED) rotation = 180;
        pose.mulPose(Axis.YP.rotationDegrees(rotation));

        mc.getEntityRenderDispatcher().submit(state, CAMERA, display.xOffset(), display.yOffset(), display.zOffset(), pose, new GhostCollector(collector, GHOST_ALPHA));
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0, 0, 0));
        output.accept(new Vector3f(1, 1.75F, 1));
    }

    @Override
    public Argument extractArgument(ItemStack stack) {
        return new Argument(DataModelItem.getStoredModel(stack), ItemDisplayContext.NONE);
    }

}
