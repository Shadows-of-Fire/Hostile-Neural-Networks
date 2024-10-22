package dev.shadowsoffire.hostilenetworks.client;

import java.util.ArrayList;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.curios.CuriosCompat;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ComponentItemHandler;

public class DeepLearnerHudRenderer implements LayeredDraw.Layer {

    public static final ResourceLocation DL_HUD = HostileNetworks.loc("textures/gui/deep_learner_hud.png");

    @Override
    public void render(GuiGraphics gfx, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !(mc.screen instanceof ChatScreen) && mc.screen != null) return;

        // Try to resolve the deep learner from the possible slot options
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(Hostile.Items.DEEP_LEARNER)) {
            stack = player.getOffhandItem();
        }

        if (!stack.is(Hostile.Items.DEEP_LEARNER) && ModList.get().isLoaded("curios")) {
            stack = CuriosCompat.getDeepLearner(player);
        }

        if (!stack.is(Hostile.Items.DEEP_LEARNER)) {
            return;
        }

        ComponentItemHandler inv = DeepLearnerItem.getItemHandler(stack);
        List<DataModelInstance> renderable = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            ItemStack model = inv.getStackInSlot(i);
            if (model.isEmpty()) continue;
            DataModelInstance cModel = new DataModelInstance(model, 0);
            if (!cModel.isValid()) continue;
            renderable.add(cModel);
        }

        if (renderable.isEmpty()) return;

        int spacing = 28;
        int x = 6;
        int y = 6;

        WeirdRenderThings.TRANSLUCENT_TRANSPARENCY.setupRenderState();
        gfx.blit(DL_HUD, 3, 3, 0, 23, 113, 1, 256, 256);
        for (int i = 0; i < renderable.size(); i++) {
            gfx.blit(DL_HUD, 3, 4 + spacing * i, 0, 24, 113, spacing, 256, 256);
            DataModelInstance cModel = renderable.get(i);
            gfx.blit(DL_HUD, x + 18, y + i * spacing + 11, 0, 0, 89, 12, 256, 256);
            int width = 87;
            if (!cModel.getTier().isMax()) {
                int prev = cModel.getTierData();
                width = Mth.ceil(width * (cModel.getData() - prev) / (float) (cModel.getNextTierData() - prev));
            }
            gfx.blit(DL_HUD, x + 19, y + i * spacing + 12, 0, 12, width, 10, 256, 256);
        }
        gfx.blit(DL_HUD, 3, 4 + spacing * renderable.size(), 0, 122, 113, 2, 256, 256);
        WeirdRenderThings.TRANSLUCENT_TRANSPARENCY.clearRenderState();

        for (int i = 0; i < renderable.size(); i++) {
            ItemStack dModel = renderable.get(i).getSourceStack();
            gfx.renderItem(dModel, x, y + i * spacing + 9);
        }

        for (int i = 0; i < renderable.size(); i++) {
            DataModelInstance cModel = renderable.get(i);
            Component comp = cModel.getTier().getComponent();
            gfx.drawString(mc.font, comp, x + 2, y + spacing * i, 0xFFFFFF, true);
            gfx.drawString(mc.font, Component.translatable("hostilenetworks.hud.model").withStyle(comp.getStyle()), x + mc.font.width(comp) + 2, y + spacing * i, 0xFFFFFF, true);
            if (!cModel.getTier().isMax()) gfx.drawString(mc.font, I18n.get("hostilenetworks.hud.kills", cModel.getKillsNeeded()), x + 21, y + 13 + i * spacing, 0xFFFFFF, true);
        }
    }

    public static void drawModel(Minecraft mc, int x, int y, ItemStack stack, DataModelInstance model, GuiGraphics gfx) {
        gfx.renderItem(stack, x, y + 9);
        Component comp = model.getTier().getComponent();
        gfx.drawString(mc.font, comp, x + 4, y, 0xFFFFFF, true);
        gfx.drawString(mc.font, Component.translatable("hostilenetworks.hud.model").withStyle(comp.getStyle()), x + mc.font.width(comp) + 4, y, 0xFFFFFF, true);
        gfx.blit(DL_HUD, x + 18, y + 10, 0, 0, 89, 12, 256, 256);
        int width = 87;
        if (!model.getTier().isMax()) {
            width = Mth.ceil(width * model.getData() / (float) model.getNextTierData());
        }
        gfx.blit(DL_HUD, x + 19, y + 11, 0, 12, width, 10, 256, 256);
    }

}
