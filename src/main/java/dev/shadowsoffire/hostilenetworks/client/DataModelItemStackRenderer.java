package dev.shadowsoffire.hostilenetworks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DataModelItemStackRenderer extends BlockEntityWithoutLevelRenderer {

    public DataModelItemStackRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static final MultiBufferSource.BufferSource GHOST_ENTITY_BUF = MultiBufferSource.immediate(new ByteBufferBuilder(256));
    private static final ModelResourceLocation DATA_MODEL_BASE = ModelResourceLocation.standalone(HostileNetworks.loc("item/data_model_base"));

    @Override
    @SuppressWarnings("deprecation")
    public void renderByItem(ItemStack stack, ItemDisplayContext type, PoseStack matrix, MultiBufferSource buf, int light, int overlay) {
        ItemRenderer irenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel base = irenderer.getItemModelShaper().getModelManager().getModel(DATA_MODEL_BASE);
        matrix.pushPose();
        if (type == ItemDisplayContext.FIXED) {
            matrix.translate(1, 1, 0);
            float scale = 0.5F;
            matrix.scale(scale, scale, scale);
            matrix.translate(-1.5F, -0.5F, 0.5F);
            matrix.mulPose(Axis.XP.rotationDegrees(90));
            matrix.mulPose(Axis.XP.rotationDegrees(90));
            matrix.translate(0, 0, -1);
        }
        else if (type != ItemDisplayContext.GUI) {
            matrix.translate(1, 1, 0);
            float scale = 0.5F;
            matrix.scale(scale, scale, scale);
            matrix.translate(-1.5F, -0.5F, 0.5F);
            matrix.mulPose(Axis.XP.rotationDegrees(90));
        }
        else {
            matrix.translate(0, -.5F, -.5F);
            matrix.mulPose(Axis.XN.rotationDegrees(75));
            matrix.mulPose(Axis.ZP.rotationDegrees(45));
            float scale = 0.9F;
            matrix.scale(scale, scale, scale);
            matrix.translate(0.775, 0, -0.0825);
        }
        irenderer.renderModelLists(base, stack, light, overlay, matrix, ItemRenderer.getFoilBufferDirect(GHOST_ENTITY_BUF, ItemBlockRenderTypes.getRenderType(stack, true), true, false));
        GHOST_ENTITY_BUF.endBatch();
        matrix.popPose();
        DynamicHolder<DataModel> model = DataModelItem.getStoredModel(stack);
        if (model.isBound()) {
            Entity ent = ClientEntityCache.computeIfAbsent(model.get().entity(), Minecraft.getInstance().level, model.get().display().nbt());
            if (Minecraft.getInstance().player != null) ent.tickCount = Minecraft.getInstance().player.tickCount;
            if (ent != null) {
                this.renderEntityInInventory(matrix, type, ent, model.get());
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void renderEntityInInventory(PoseStack matrix, ItemDisplayContext type, Entity entity, DataModel model) {
        matrix.pushPose();
        matrix.translate(0.5, 0.5, 0.5);
        float scale = model.display().scale();
        if (type == ItemDisplayContext.FIXED) {
            matrix.translate(0, -0.5, 0);
            scale *= 0.4F;
            matrix.scale(scale, scale, scale);
            matrix.translate(0, 1.45, 0);
            matrix.mulPose(Axis.XN.rotationDegrees(90));
            matrix.mulPose(Axis.YN.rotationDegrees(180));
        }
        else if (type == ItemDisplayContext.GUI) {
            matrix.translate(0, -0.5, 0);
            scale *= 0.4F;
            matrix.scale(scale, scale, scale);
            matrix.translate(0, 0.45, 0);
        }
        else {
            scale *= 0.25F;
            matrix.scale(scale, scale, scale);
            matrix.translate(0, 0.12 + 0.05 * Math.sin((entity.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) / 12), 0);
        }

        float rotation = -30;
        if (type == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || type == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) rotation = 30;
        if (type == ItemDisplayContext.FIXED) rotation = 180;
        matrix.mulPose(Axis.YP.rotationDegrees(rotation));
        entity.setYRot(0);

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = entity.getYRot();
            living.yHeadRot = entity.getYRot();
            living.yHeadRotO = entity.getYRot();
        }

        EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderermanager.setRenderShadow(false);
        MultiBufferSource.BufferSource rtBuffer = GHOST_ENTITY_BUF;
        WeirdRenderThings.translucent = true;
        RenderSystem.runAsFancy(() -> {
            entityrenderermanager.render(entity, model.display().xOffset(), model.display().yOffset(), model.display().zOffset(), 0.0F, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), matrix,
                new WrappedRTBuffer(rtBuffer), 15728880);
        });
        rtBuffer.endBatch();
        WeirdRenderThings.translucent = false;
        entityrenderermanager.setRenderShadow(true);
        matrix.popPose();
    }

}
