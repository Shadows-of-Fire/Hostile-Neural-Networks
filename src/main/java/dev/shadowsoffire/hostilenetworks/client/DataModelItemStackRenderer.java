package dev.shadowsoffire.hostilenetworks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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
            DisplayEntity display = model.get().displayEntity(Minecraft.getInstance().level);
            Entity ent = ClientEntityCache.computeIfAbsent(display, Minecraft.getInstance().level);
            if (Minecraft.getInstance().player != null) {
                ent.tickCount = Minecraft.getInstance().player.tickCount;
            }
            if (ent != null) {
                renderEntityInInventory(matrix, type, ent, display);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static void renderEntityInInventory(PoseStack matrix, ItemDisplayContext type, Entity entity, DisplayEntity display) {
        matrix.pushPose();
        matrix.translate(0.5, 0.5, 0.5);
        float scale = display.scale();
        if (type == ItemDisplayContext.FIXED) {
            scale *= 0.4F;
            matrix.scale(scale, scale, scale);
            matrix.mulPose(Axis.XN.rotationDegrees(90));
            matrix.mulPose(Axis.YN.rotationDegrees(180));
        }
        else if (type == ItemDisplayContext.GUI) {
            scale *= 0.4F;
            matrix.scale(scale, scale, scale);
            matrix.translate(0, -0.32 / scale, 0);
        }
        else {
            scale *= 0.25F;
            matrix.scale(scale, scale, scale);
            double yTranslation = 0.12 + 0.05 * Math.sin((entity.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) / 12);
            yTranslation = (yTranslation * 0.25) / scale;
            matrix.translate(0, yTranslation, 0);
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

        float partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

        // When rendering an item entity, we want to prevent any bobbing or spinning from occurring.
        // To do that, we have to apply the inverse transforms that would normally be applied so when the real ones apply (in ItemEntityRenderer), they cancel out.
        if (entity instanceof ItemEntity item) {
            ItemStack itemstack = item.getItem();
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            BakedModel bakedmodel = itemRenderer.getModel(itemstack, entity.level(), null, entity.getId());

            boolean shouldBob = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(itemstack).shouldBobAsEntity(itemstack);
            float f1 = shouldBob ? Mth.sin(((float) item.getAge() + partialTicks) / 10.0F + item.bobOffs) * 0.1F + 0.1F : 0;
            float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();

            float f3 = item.getSpin(partialTicks);
            matrix.mulPose(Axis.YP.rotation(-f3));

            matrix.translate(0.0F, -(f1 + 0.25F * f2), 0.0F);
        }

        EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderermanager.setRenderShadow(false);
        MultiBufferSource.BufferSource rtBuffer = GHOST_ENTITY_BUF;
        WeirdRenderThings.translucent = true;
        RenderSystem.runAsFancy(() -> {
            entityrenderermanager.render(entity, display.xOffset(), display.yOffset(), display.zOffset(), 0.0F, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), matrix,
                new WrappedRTBuffer(rtBuffer), 0xF000F0);
        });
        rtBuffer.endBatch();
        WeirdRenderThings.translucent = false;
        entityrenderermanager.setRenderShadow(true);
        matrix.popPose();
    }

}
