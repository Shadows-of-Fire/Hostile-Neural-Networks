package shadows.hostilenetworks.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import shadows.hostilenetworks.HostileClient;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.CachedModel;
import shadows.hostilenetworks.data.DataModel;

public class DataModelItemStackRenderer extends ItemStackTileEntityRenderer {

	private static final IRenderTypeBuffer.Impl GHOST_ENTITY_BUF = IRenderTypeBuffer.immediate(new BufferBuilder(256));

	private static final ResourceLocation DATA_MODEL_BASE = new ResourceLocation(HostileNetworks.MODID, "item/data_model_base");

	@Override
	public void renderByItem(ItemStack stack, TransformType type, MatrixStack matrix, IRenderTypeBuffer buf, int light, int overlay) {
		CachedModel model = new CachedModel(stack, 0);
		ItemRenderer irenderer = Minecraft.getInstance().getItemRenderer();
		IBakedModel base = irenderer.getItemModelShaper().getModelManager().getModel(DATA_MODEL_BASE);
		matrix.pushPose();
		if (type != TransformType.GUI) {
			matrix.translate(1, 1, 0);
			float scale = 0.5F;
			matrix.scale(scale, scale, scale);
			matrix.translate(-1.5F, -0.5F, 0.5F);
			matrix.mulPose(Vector3f.XP.rotationDegrees(90));
		} else {
			matrix.translate(0, -.5F, -.5F);
			matrix.mulPose(Vector3f.XN.rotationDegrees(75));
			matrix.mulPose(Vector3f.ZP.rotationDegrees(45));
			float scale = 0.9F;
			matrix.scale(scale, scale, scale);
			matrix.translate(0.775, 0, -0.0825);
		}
		irenderer.renderModelLists(base, stack, light, overlay, matrix, ItemRenderer.getFoilBufferDirect(GHOST_ENTITY_BUF, RenderTypeLookup.getRenderType(stack, true), true, false));
		GHOST_ENTITY_BUF.endBatch();
		matrix.popPose();
		if (model.getModel() != null) {
			LivingEntity ent = model.getEntity(Minecraft.getInstance().level);
			if (ent != null) {
				this.renderEntityInInventory(matrix, type, ent, model.getModel());
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void renderEntityInInventory(MatrixStack matrix, TransformType type, LivingEntity pLivingEntity, DataModel model) {
		RenderSystem.pushMatrix();
		matrix.pushPose();
		matrix.translate(0.5, 0.5, 0.5);
		if (type == TransformType.GUI) {
			matrix.translate(0, -0.5, 0);
			float scale = 0.4F;
			scale *= model.getScale();
			matrix.scale(scale, scale, scale);
			matrix.translate(0, 0.45, 0);
		} else {
			float scale = 0.25F;
			scale *= model.getScale();
			matrix.scale(scale, scale, scale);
			matrix.translate(0, 0.12 + 0.05 * Math.sin((HostileClient.clientTicks + Minecraft.getInstance().getDeltaFrameTime()) / 12), 0);
		}

		float rotation = -30;
		if (type == TransformType.FIRST_PERSON_LEFT_HAND || type == TransformType.THIRD_PERSON_LEFT_HAND) rotation = 30;
		if (type == TransformType.FIXED) rotation = 180;
		matrix.mulPose(Vector3f.YP.rotationDegrees(rotation));
		pLivingEntity.yRot = 0;
		pLivingEntity.yBodyRot = pLivingEntity.yRot;
		pLivingEntity.yHeadRot = pLivingEntity.yRot;
		pLivingEntity.yHeadRotO = pLivingEntity.yRot;
		EntityRendererManager entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
		entityrenderermanager.setRenderShadow(false);
		IRenderTypeBuffer.Impl rtBuffer = GHOST_ENTITY_BUF;
		WeirdRenderThings.fullbright = true;
		WeirdRenderThings.translucent = true;
		RenderSystem.runAsFancy(() -> {
			entityrenderermanager.render(pLivingEntity, model.getXOffset(), model.getYOffset(), model.getZOffset(), 0.0F, 1, matrix, new WrappedRTBuffer(rtBuffer), 15728880);
		});
		rtBuffer.endBatch();
		WeirdRenderThings.translucent = false;
		WeirdRenderThings.fullbright = false;
		entityrenderermanager.setRenderShadow(true);
		RenderSystem.popMatrix();
		matrix.popPose();
	}

}
