package dev.shadowsoffire.hostilenetworks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(bus = Bus.FORGE, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class DataModelItemStackRenderer extends BlockEntityWithoutLevelRenderer {

    public DataModelItemStackRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static final MultiBufferSource.BufferSource GHOST_ENTITY_BUF = MultiBufferSource.immediate(new BufferBuilder(256));
    private static final ResourceLocation DATA_MODEL_BASE = new ResourceLocation(HostileNetworks.MODID, "item/data_model_base");

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
            LivingEntity ent = ClientEntityCache.computeIfAbsent(model.get().type(), Minecraft.getInstance().level, model.get().displayNbt());
            if (Minecraft.getInstance().player != null) ent.tickCount = Minecraft.getInstance().player.tickCount;
            if (ent != null) {
                this.renderEntityInInventory(matrix, type, ent, model.get());
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void renderEntityInInventory(PoseStack matrix, ItemDisplayContext type, LivingEntity pLivingEntity, DataModel model) {
        matrix.pushPose();
        matrix.translate(0.5, 0.5, 0.5);
        if (type == ItemDisplayContext.FIXED) {
            matrix.translate(0, -0.5, 0);
            float scale = 0.4F;
            scale *= model.guiScale();
            matrix.scale(scale, scale, scale);
            matrix.translate(0, 1.45, 0);
            matrix.mulPose(Axis.XN.rotationDegrees(90));
            matrix.mulPose(Axis.YN.rotationDegrees(180));
        }
        else if (type == ItemDisplayContext.GUI) {
            matrix.translate(0, -0.5, 0);
            float scale = 0.4F;
            scale *= model.guiScale();
            matrix.scale(scale, scale, scale);
            matrix.translate(0, 0.45, 0);
        }
        else {
            float scale = 0.25F;
            scale *= model.guiScale();
            matrix.scale(scale, scale, scale);
            matrix.translate(0, 0.12 + 0.05 * Math.sin((pLivingEntity.tickCount + Minecraft.getInstance().getDeltaFrameTime()) / 12), 0);
        }

        float rotation = -30;
        if (type == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || type == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) rotation = 30;
        if (type == ItemDisplayContext.FIXED) rotation = 180;
        matrix.mulPose(Axis.YP.rotationDegrees(rotation));
        pLivingEntity.setYRot(0);
        pLivingEntity.yBodyRot = pLivingEntity.getYRot();
        pLivingEntity.yHeadRot = pLivingEntity.getYRot();
        pLivingEntity.yHeadRotO = pLivingEntity.getYRot();
        EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderermanager.setRenderShadow(false);
        MultiBufferSource.BufferSource rtBuffer = GHOST_ENTITY_BUF;
        WeirdRenderThings.translucent = true;
        RenderSystem.runAsFancy(() -> {
            entityrenderermanager.render(pLivingEntity, model.guiXOff(), model.guiYOff(), model.guiZOff(), 0.0F, Minecraft.getInstance().getDeltaFrameTime(), matrix, new WrappedRTBuffer(rtBuffer), 15728880);
        });
        rtBuffer.endBatch();
        WeirdRenderThings.translucent = false;
        entityrenderermanager.setRenderShadow(true);
        matrix.popPose();
    }

}
