package dev.shadowsoffire.hostilenetworks.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.RenderStateShard;

public class WeirdRenderThings {

    public static boolean translucent = false;

    public static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    public static void setup() {
        if (translucent) {
            TRANSLUCENT_TRANSPARENCY.setupRenderState();
        }
    }

    public static void cleanup() {
        if (translucent) {
            TRANSLUCENT_TRANSPARENCY.clearRenderState();
        }
    }
}
