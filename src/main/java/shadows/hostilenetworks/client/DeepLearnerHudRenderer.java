package shadows.hostilenetworks.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.items.ItemStackHandler;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.CachedModel;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.item.DeepLearnerItem;

@EventBusSubscriber(value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class DeepLearnerHudRenderer {

	private static final ResourceLocation DL_HUD = new ResourceLocation(HostileNetworks.MODID, "textures/gui/deep_learner_hud.png");

	@SubscribeEvent
	public static void render(RenderGameOverlayEvent.Post e) {
		if (e.getType() != ElementType.ALL) return;
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || !(mc.screen instanceof ChatScreen) && mc.screen != null) return;

		ItemStack stack = player.getMainHandItem();
		if (stack.getItem() != Hostile.Items.DEEP_LEARNER) stack = player.getOffhandItem();
		if (stack.getItem() != Hostile.Items.DEEP_LEARNER) return;

		ItemStackHandler inv = DeepLearnerItem.getItemHandler(stack);
		List<Pair<CachedModel, ItemStack>> renderable = new ArrayList<>(4);
		for (int i = 0; i < 4; i++) {
			ItemStack model = inv.getStackInSlot(i);
			if (model.isEmpty()) continue;
			CachedModel cModel = new CachedModel(model, 0);
			if (cModel.getModel() == null) continue;
			renderable.add(Pair.of(cModel, model));
		}

		if (renderable.isEmpty()) return;

		int spacing = 28;
		int x = 6;
		int y = 6;

		WeirdRenderThings.TRANSLUCENT_TRANSPARENCY.setupRenderState();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, DL_HUD);
		GuiComponent.blit(e.getMatrixStack(), 3, 3, 0, 23, 113, 1, 256, 256);
		for (int i = 0; i < renderable.size(); i++) {
			GuiComponent.blit(e.getMatrixStack(), 3, 4 + spacing * i, 0, 24, 113, spacing, 256, 256);
			CachedModel cModel = renderable.get(i).getLeft();
			GuiComponent.blit(e.getMatrixStack(), x + 18, y + i * spacing + 10, 0, 0, 89, 12, 256, 256);
			int width = 87;
			if (cModel.getTier() != ModelTier.SELF_AWARE) {
				int prev = cModel.getTierData();
				width = Mth.ceil(width * (cModel.getData() - prev) / (float) (cModel.getNextTierData() - prev));
			}
			GuiComponent.blit(e.getMatrixStack(), x + 19, y + i * spacing + 11, 0, 12, width, 10, 256, 256);
		}
		GuiComponent.blit(e.getMatrixStack(), 3, 4 + spacing * renderable.size(), 0, 122, 113, 2, 256, 256);
		WeirdRenderThings.TRANSLUCENT_TRANSPARENCY.clearRenderState();

		for (int i = 0; i < renderable.size(); i++) {
			ItemStack dModel = renderable.get(i).getRight();
			mc.getItemRenderer().renderAndDecorateItem(dModel, x, y + i * spacing + 9);
		}

		for (int i = 0; i < renderable.size(); i++) {
			CachedModel cModel = renderable.get(i).getLeft();
			Component comp = cModel.getTier().getComponent();
			mc.font.drawShadow(e.getMatrixStack(), comp, x + 4, y + spacing * i, 0xFFFFFF);
			mc.font.drawShadow(e.getMatrixStack(), new TranslatableComponent("hostilenetworks.hud.model"), x + mc.font.width(comp) + 4, y + spacing * i, 0xFFFFFF);
			if (cModel.getTier() != ModelTier.SELF_AWARE) mc.font.drawShadow(e.getMatrixStack(), I18n.get("hostilenetworks.hud.kills", cModel.getKillsNeeded()), x + 21, y + 12 + i * spacing, 0xFFFFFF);
		}
	}

	public static void drawModel(Minecraft mc, int x, int y, ItemStack stack, CachedModel model) {
		mc.getItemRenderer().renderAndDecorateItem(stack, x, y + 9);
		Component comp = model.getTier().getComponent();
		PoseStack matrix = new PoseStack();
		mc.font.drawShadow(matrix, comp, x + 4, y, 0xFFFFFF);
		mc.font.drawShadow(matrix, new TranslatableComponent("hostilenetworks.hud.model"), x + mc.font.width(comp) + 4, y, 0xFFFFFF);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, DL_HUD);
		GuiComponent.blit(matrix, x + 18, y + 10, 0, 0, 89, 12, 256, 256);
		int width = 87;
		if (model.getTier() != ModelTier.SELF_AWARE) {
			width = Mth.ceil(width * model.getData() / (float) model.getNextTierData());
		}
		GuiComponent.blit(matrix, x + 19, y + 11, 0, 12, width, 10, 256, 256);
	}

}
