package dev.shadowsoffire.hostilenetworks.client;

import java.util.ArrayList;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.client.Offset.Box;
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
    public static final ResourceLocation DL_HUD_BG = HostileNetworks.loc("dl_hud_bg");
    public static final int SPACING = 28;

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

        gfx.pose().pushPose();

        Offset offset = HostileConfig.deepLearnerOffset;
        Box window = new Box(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        Box element = new Box(119, 10 + SPACING * renderable.size());
        offset.apply(gfx.pose(), window, element);

        int x = 6;
        int y = 6;

        WeirdRenderThings.TRANSLUCENT_TRANSPARENCY.setupRenderState();
        // Render the background (as a sprite for autoscaling) and the progress bars
        gfx.blitSprite(DL_HUD_BG, 3, 3, 113, 5 + SPACING * renderable.size());
        for (int i = 0; i < renderable.size(); i++) {
            DataModelInstance cModel = renderable.get(i);
            gfx.blit(DL_HUD, x + 18, y + i * SPACING + 11, 0, 0, 89, 12, 256, 256);
            int width = 87;
            if (!cModel.getTier().isMax()) {
                int prev = cModel.getTierData();
                width = Mth.ceil(width * (cModel.getData() - prev) / (float) (cModel.getNextTierData() - prev));
            }
            gfx.blit(DL_HUD, x + 19, y + i * SPACING + 12, 0, 12, width, 10, 256, 256);
        }
        WeirdRenderThings.TRANSLUCENT_TRANSPARENCY.clearRenderState();

        // Then the model items
        for (int i = 0; i < renderable.size(); i++) {
            ItemStack dModel = renderable.get(i).getSourceStack();
            gfx.renderItem(dModel, x, y + i * SPACING + 9);
        }

        // Then all the text
        for (int i = 0; i < renderable.size(); i++) {
            DataModelInstance cModel = renderable.get(i);
            Component comp = cModel.getTier().getComponent();
            gfx.drawString(mc.font, comp, x + 2, y + SPACING * i, 0xFFFFFF, true);
            gfx.drawString(mc.font, Component.translatable("hostilenetworks.hud.model").withStyle(comp.getStyle()), x + mc.font.width(comp) + 2, y + SPACING * i, 0xFFFFFF, true);
            if (!cModel.getTier().isMax()) gfx.drawString(mc.font, I18n.get("hostilenetworks.hud.kills", cModel.getKillsNeeded()), x + 21, y + 13 + i * SPACING, 0xFFFFFF, true);
        }

        gfx.pose().popPose();
    }

}
