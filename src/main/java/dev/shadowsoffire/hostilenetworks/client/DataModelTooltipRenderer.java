package dev.shadowsoffire.hostilenetworks.client;

import org.joml.Matrix4f;

import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public record DataModelTooltipRenderer(DataModelInstance model) implements ClientTooltipComponent {

    @Override
    public int getHeight() {
        return 30;
    }

    @Override
    public int getWidth(Font font) {
        Component tierName = model.getTier().getComponent();
        return Math.max(107, font.width(tierName) + font.width(I18n.get("hostilenetworks.hud.model")));
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics gfx) {
        gfx.renderItem(model.getSourceStack(), x, y + 9);
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 0);
        gfx.blit(DeepLearnerHudRenderer.DL_HUD, x + 18, y + 10, 0, 0, 89, 12, 256, 256);
        int width = 87;
        if (!model.getTier().isMax()) {
            int prev = model.getTierData();
            width = Mth.ceil(width * (model.getData() - prev) / (float) (model.getNextTierData() - prev));
        }
        gfx.blit(DeepLearnerHudRenderer.DL_HUD, x + 19, y + 11, 0, 12, width, 10, 256, 256);
        gfx.pose().popPose();
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f srcMatrix, BufferSource bufferSource) {
        Component tierName = model.getTier().getComponent();
        // Need to translate text slightly on the Z-index to ensure it's above the graphics.
        Matrix4f mat = new Matrix4f(srcMatrix);
        mat.translate(0, 0, 0.05F);
        font.drawInBatch(tierName, x, y, 0xFFFFFFFF, true, mat, bufferSource, DisplayMode.NORMAL, 0, 15728880);
        font.drawInBatch(Component.translatable("hostilenetworks.hud.model"), x + font.width(tierName), y, 0xFFFFFFFF, true, mat, bufferSource, DisplayMode.NORMAL, 0, 15728880);

        if (!model.getTier().isMax()) {
            font.drawInBatch(I18n.get("hostilenetworks.hud.kills", model.getKillsNeeded()), x + 21, y + 12, 0xFFFFFFFF, true, mat, bufferSource, DisplayMode.NORMAL, 0, 15728880);
        }

    }

}
