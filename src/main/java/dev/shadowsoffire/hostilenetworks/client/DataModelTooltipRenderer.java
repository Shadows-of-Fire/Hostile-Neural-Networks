package dev.shadowsoffire.hostilenetworks.client;

import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public record DataModelTooltipRenderer(DataModelInstance model) implements ClientTooltipComponent {

    @Override
    public int getHeight(Font font) {
        return 29;
    }

    @Override
    public int getWidth(Font font) {
        Component tierName = model.getTier().getComponent();
        return Math.max(107, font.width(tierName) + font.width(I18n.get("hostilenetworks.hud.model")));
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor gfx) {
        gfx.item(model.getSourceStack(), x, y + 10);
        gfx.blit(RenderPipelines.GUI_TEXTURED, DeepLearnerHudRenderer.DL_HUD, x + 20, y + 12, 0F, 0F, 89, 12, 256, 256);
        int width = 87;
        if (!model.getTier().isMax()) {
            int prev = model.getTierData();
            width = Mth.ceil(width * (model.getData() - prev) / (float) (model.getNextTierData() - prev));
        }
        gfx.blit(RenderPipelines.GUI_TEXTURED, DeepLearnerHudRenderer.DL_HUD, x + 21, y + 13, 0F, 12F, width, 10, 256, 256);
    }

    @Override
    public void extractText(GuiGraphicsExtractor gfx, Font font, int x, int y) {
        Component tierName = model.getTier().getComponent();
        gfx.text(font, tierName, x, y, 0xFFFFFFFF, true);
        gfx.text(font, Component.translatable("hostilenetworks.hud.model").withStyle(tierName.getStyle()), x + font.width(tierName), y, 0xFFFFFFFF, true);
        if (!model.getTier().isMax()) {
            gfx.text(font, I18n.get("hostilenetworks.hud.kills", model.getActionsNeeded()), x + 23, y + 14, 0xFFFFFFFF, true);
        }
    }
}
