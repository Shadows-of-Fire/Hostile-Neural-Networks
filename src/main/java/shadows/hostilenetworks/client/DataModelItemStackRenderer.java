package shadows.hostilenetworks.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import shadows.hostilenetworks.HostileClient;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.item.DataModelItem;

@EventBusSubscriber(bus = Bus.FORGE, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class DataModelItemStackRenderer extends BlockEntityWithoutLevelRenderer {

	public DataModelItemStackRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	private static final MultiBufferSource.BufferSource GHOST_ENTITY_BUF = MultiBufferSource.immediate(new BufferBuilder(256));
	private static final ResourceLocation DATA_MODEL_BASE = new ResourceLocation(HostileNetworks.MODID, "item/data_model_base");
	private static final Map<EntityType<?>, LivingEntity> CACHE = new WeakHashMap<>();

	@Override
	public void renderByItem(ItemStack stack, TransformType type, PoseStack matrix, MultiBufferSource buf, int light, int overlay) {
		ItemRenderer irenderer = Minecraft.getInstance().getItemRenderer();
		BakedModel base = irenderer.getItemModelShaper().getModelManager().getModel(DATA_MODEL_BASE);
		matrix.pushPose();
		if (type == TransformType.FIXED) {
			matrix.translate(1, 1, 0);
			float scale = 0.5F;
			matrix.scale(scale, scale, scale);
			matrix.translate(-1.5F, -0.5F, 0.5F);
			matrix.mulPose(Vector3f.XP.rotationDegrees(90));
			matrix.mulPose(Vector3f.XP.rotationDegrees(90));
			matrix.translate(0, 0, -1);
		} else if (type != TransformType.GUI) {
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
		irenderer.renderModelLists(base, stack, light, overlay, matrix, ItemRenderer.getFoilBufferDirect(GHOST_ENTITY_BUF, ItemBlockRenderTypes.getRenderType(stack, true), true, false));
		GHOST_ENTITY_BUF.endBatch();
		matrix.popPose();
		DataModel model = DataModelItem.getStoredModel(stack);
		if (model != null) {
			LivingEntity ent = ClientEntityCache.computeIfAbsent(model.getType(), Minecraft.getInstance().level, model.getDisplayNbt());
			if (Minecraft.getInstance().player != null) ent.tickCount = Minecraft.getInstance().player.tickCount;
			if (ent != null) {
				this.renderEntityInInventory(matrix, type, ent, model);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void renderEntityInInventory(PoseStack matrix, TransformType type, LivingEntity pLivingEntity, DataModel model) {
		matrix.pushPose();
		matrix.translate(0.5, 0.5, 0.5);
		if (type == TransformType.FIXED) {
			matrix.translate(0, -0.5, 0);
			float scale = 0.4F;
			scale *= model.getScale();
			matrix.scale(scale, scale, scale);
			matrix.translate(0, 1.45, 0);
			matrix.mulPose(Vector3f.XN.rotationDegrees(90));
			matrix.mulPose(Vector3f.YN.rotationDegrees(180));
		} else if (type == TransformType.GUI) {
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
		pLivingEntity.setYRot(0);
		pLivingEntity.yBodyRot = pLivingEntity.getYRot();
		pLivingEntity.yHeadRot = pLivingEntity.getYRot();
		pLivingEntity.yHeadRotO = pLivingEntity.getYRot();
		EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
		entityrenderermanager.setRenderShadow(false);
		MultiBufferSource.BufferSource rtBuffer = GHOST_ENTITY_BUF;
		WeirdRenderThings.fullbright_tesr = true;
		WeirdRenderThings.translucent = true;
		RenderSystem.runAsFancy(() -> {
			entityrenderermanager.render(pLivingEntity, model.getXOffset(), model.getYOffset(), model.getZOffset(), 0.0F, Minecraft.getInstance().getDeltaFrameTime(), matrix, new WrappedRTBuffer(rtBuffer), 15728880);
		});
		rtBuffer.endBatch();
		WeirdRenderThings.translucent = false;
		WeirdRenderThings.fullbright_tesr = false;
		entityrenderermanager.setRenderShadow(true);
		matrix.popPose();
	}

	@SubscribeEvent
	public static void join(EntityJoinWorldEvent e) {
		if (e.getEntity() == Minecraft.getInstance().player) CACHE.clear();
	}

}
